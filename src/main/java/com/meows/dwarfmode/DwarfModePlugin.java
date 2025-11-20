package com.meows.dwarfmode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class DwarfModePlugin extends JavaPlugin implements Listener {

    // ============================================
    // БОНУСЫ ЗА НАХОЖДЕНИЕ ПОД ЗЕМЛЕЙ
    // ============================================
    private boolean undergroundBonusesEnabled = true;
    private TreeMap<Integer, List<PotionEffect>> undergroundEffects = new TreeMap<>();
    private int undergroundMinDepth = 0; // Минимальная глубина для бонусов (Y координата)

    // ============================================
    // ШТРАФЫ ЗА СОЛНЕЧНЫЙ СВЕТ
    // ============================================
    private boolean sunlightPenaltiesEnabled = true;
    private boolean sunlightBurn = true; // Гномы горят на солнце
    private List<PotionEffect> sunlightEffects = new ArrayList<>();
    private long dayTimeStart = 0; // Начало дня (в тиках)
    private long dayTimeEnd = 12000; // Конец дня (в тиках)
    private int sunlightEffectDuration = 100; // Продолжительность эффектов (в тиках)

    // ============================================
    // СЛЕПОТА НА ОТКРЫТОМ НЕБЕ
    // ============================================
    private boolean skyBlindnessEnabled = false; // Слепота на открытом небе (всегда, не только днем)

    // ============================================
    // ШТРАФЫ ЗА НАХОЖДЕНИЕ НА ПОВЕРХНОСТИ
    // ============================================
    private boolean surfacePenaltiesEnabled = true;
    private int surfaceLevel = 64; // Высота, считающаяся "поверхностью"
    private List<PotionEffect> surfaceEffects = new ArrayList<>();
    private int surfaceEffectDuration = 100; // Продолжительность эффектов (в тиках)
    private boolean surfaceDamageEnabled = false;
    private int surfaceDamage = 1;
    private int surfaceDamageInterval = 100; // В тиках
    private Map<UUID, Integer> surfaceDamageTicks = new HashMap<>(); // Кеш для урона со временем

    // Список миров, в которых применяются эффекты
    private List<String> enabledWorlds = new ArrayList<>();

    // Кеш последней проверенной высоты для каждого игрока (для оптимизации)
    private Map<UUID, Integer> lastCheckedHeight = new HashMap<>();

    // Задача для периодической проверки
    private BukkitTask checkTask = null;

    // Интервал проверки в тиках (20 тиков = 1 секунда, 40 = 2 секунды)
    private int checkInterval = 40;

    @Override
    public void onEnable() {
        // Регистрация событий
        this.getServer().getPluginManager().registerEvents(this, this);

        // Загрузка конфига
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
        }

        // Загрузка конфигурации
        loadConfig();

        // Запуск периодической проверки
        startPeriodicCheck();

        getLogger().info("DwarfMode включен!");
    }

    @Override
    public void onDisable() {
        // Остановка задачи при выключении плагина
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        getLogger().info("DwarfMode выключен!");
    }

    /**
     * Загрузка и валидация конфигурации
     */
    private void loadConfig() {
        try {
            reloadConfig();

            // Загрузка интервала проверки
            try {
                checkInterval = getConfig().getInt("checkInterval", 40);
                if (checkInterval < 1)
                    checkInterval = 40;
            } catch (Exception e) {
                getLogger().warning("Ошибка при чтении checkInterval, используется значение по умолчанию: 40");
                checkInterval = 40;
            }

            // Загрузка списка разрешенных миров
            loadEnabledWorlds();

            // Загрузка бонусов под землей
            loadUndergroundBonuses();

            // Загрузка штрафов за солнечный свет
            loadSunlightPenalties();

            // Загрузка слепоты на открытом небе
            loadSkyBlindness();

            // Загрузка штрафов на поверхности
            loadSurfacePenalties();

            getLogger().info("Конфигурация загружена успешно!");

        } catch (Exception e) {
            getLogger().severe("Критическая ошибка при загрузке конфига! Используются значения по умолчанию.");
            e.printStackTrace();
        }
    }

    /**
     * Загрузка списка разрешенных миров
     */
    private void loadEnabledWorlds() {
        enabledWorlds.clear();

        try {
            List<?> worldList = getConfig().getList("enabledWorlds");
            if (worldList == null || worldList.isEmpty()) {
                // Значение по умолчанию - только world
                enabledWorlds.add("world");
                getLogger().info("Список миров не найден, используется значение по умолчанию: world");
                return;
            }

            for (Object worldObj : worldList) {
                if (worldObj instanceof String) {
                    String worldName = ((String) worldObj).trim();
                    if (!worldName.isEmpty()) {
                        enabledWorlds.add(worldName);
                    }
                }
            }

            if (enabledWorlds.isEmpty()) {
                enabledWorlds.add("world");
                getLogger().warning("Список миров пуст, используется значение по умолчанию: world");
            } else {
                getLogger().info("Загружено " + enabledWorlds.size() + " разрешенных миров: "
                        + String.join(", ", enabledWorlds));
            }

        } catch (Exception e) {
            getLogger().severe("Ошибка при загрузке списка миров: " + e.getMessage());
            enabledWorlds.clear();
            enabledWorlds.add("world"); // Значение по умолчанию
        }
    }

    /**
     * Загрузка бонусов за нахождение под землей
     */
    private void loadUndergroundBonuses() {
        undergroundEffects.clear();

        try {
            ConfigurationSection section = getConfig().getConfigurationSection("undergroundBonuses");
            if (section == null) {
                getLogger().warning("Секция undergroundBonuses не найдена в конфиге!");
                undergroundBonusesEnabled = false;
                return;
            }

            undergroundBonusesEnabled = section.getBoolean("enabled", true);
            undergroundMinDepth = section.getInt("minDepth", 0);

            ConfigurationSection depthSection = section.getConfigurationSection("depthEffects");
            if (depthSection == null) {
                getLogger().warning("Секция depthEffects не найдена!");
                return;
            }

            for (String depthKey : depthSection.getKeys(false)) {
                try {
                    int depth = Integer.parseInt(depthKey);
                    if (depth < -64 || depth > 256) {
                        getLogger()
                                .warning("Глубина " + depth + " вне допустимого диапазона (-64 до 256). Пропускаем.");
                        continue;
                    }

                    List<?> effectList = depthSection.getList(depthKey);
                    if (effectList == null || effectList.isEmpty()) {
                        continue;
                    }

                    // Для подземных бонусов используем продолжительность 200 тиков по умолчанию
                    List<PotionEffect> effects = parseEffects(effectList, depth, 100);
                    if (!effects.isEmpty()) {
                        undergroundEffects.put(depth, effects);
                    }

                } catch (Exception e) {
                    getLogger().warning("Ошибка при обработке глубины " + depthKey + ": " + e.getMessage());
                }
            }

            getLogger().info("Загружено " + undergroundEffects.size() + " уровней бонусов под землей.");

        } catch (Exception e) {
            getLogger().severe("Ошибка при загрузке бонусов под землей: " + e.getMessage());
            undergroundBonusesEnabled = false;
        }
    }

    /**
     * Загрузка штрафов за солнечный свет
     */
    private void loadSunlightPenalties() {
        sunlightEffects.clear();

        try {
            ConfigurationSection section = getConfig().getConfigurationSection("sunlightPenalties");
            if (section == null) {
                getLogger().warning("Секция sunlightPenalties не найдена в конфиге!");
                sunlightPenaltiesEnabled = false;
                return;
            }

            sunlightPenaltiesEnabled = section.getBoolean("enabled", true);
            sunlightBurn = section.getBoolean("burnInSunlight", true);
            dayTimeStart = section.getLong("dayTimeStart", 0);
            dayTimeEnd = section.getLong("dayTimeEnd", 12000);
            sunlightEffectDuration = section.getInt("duration", 100);

            List<?> effectList = section.getList("sunlightEffects");
            if (effectList != null && !effectList.isEmpty()) {
                sunlightEffects = parseEffects(effectList, 0, sunlightEffectDuration);
            }

            getLogger().info("Штрафы за солнечный свет загружены. Эффектов: " + sunlightEffects.size());

        } catch (Exception e) {
            getLogger().severe("Ошибка при загрузке штрафов за солнечный свет: " + e.getMessage());
            sunlightPenaltiesEnabled = false;
        }
    }

    /**
     * Загрузка настройки слепоты на открытом небе
     */
    private void loadSkyBlindness() {
        try {
            skyBlindnessEnabled = getConfig().getBoolean("skyBlindness.enabled", false);
            getLogger().info("Слепота на открытом небе: " + (skyBlindnessEnabled ? "включена" : "выключена"));
        } catch (Exception e) {
            getLogger().warning("Ошибка при загрузке настройки слепоты на открытом небе: " + e.getMessage());
            skyBlindnessEnabled = false;
        }
    }

    /**
     * Загрузка штрафов на поверхности
     */
    private void loadSurfacePenalties() {
        surfaceEffects.clear();

        try {
            ConfigurationSection section = getConfig().getConfigurationSection("surfacePenalties");
            if (section == null) {
                getLogger().warning("Секция surfacePenalties не найдена в конфиге!");
                surfacePenaltiesEnabled = false;
                return;
            }

            surfacePenaltiesEnabled = section.getBoolean("enabled", true);
            surfaceLevel = section.getInt("surfaceLevel", 64);
            surfaceEffectDuration = section.getInt("duration", 100);

            List<?> effectList = section.getList("surfaceEffects");
            if (effectList != null && !effectList.isEmpty()) {
                surfaceEffects = parseEffects(effectList, 0, surfaceEffectDuration);
            }

            // Загрузка настроек урона со временем
            ConfigurationSection damageSection = section.getConfigurationSection("damageOverTime");
            if (damageSection != null) {
                surfaceDamageEnabled = damageSection.getBoolean("enabled", false);
                surfaceDamage = damageSection.getInt("damage", 1);
                surfaceDamageInterval = damageSection.getInt("interval", 100);
            }

            getLogger().info("Штрафы на поверхности загружены. Эффектов: " + surfaceEffects.size());

        } catch (Exception e) {
            getLogger().severe("Ошибка при загрузке штрафов на поверхности: " + e.getMessage());
            surfacePenaltiesEnabled = false;
        }
    }

    /**
     * Парсинг списка эффектов из конфига
     */
    private List<PotionEffect> parseEffects(List<?> effectList, int depth, int duration) {
        List<PotionEffect> effects = new ArrayList<>();

        for (Object effectObj : effectList) {
            try {
                if (!(effectObj instanceof String)) {
                    continue;
                }

                String effectString = (String) effectObj;
                String[] parts = effectString.split(":");

                if (parts.length < 2) {
                    getLogger().warning("Неверный формат эффекта: " + effectString + " (ожидается: EffectName:Level)");
                    continue;
                }

                String effectName = parts[0].trim();
                int level;

                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    getLogger().warning("Неверный уровень эффекта: " + parts[1] + " в эффекте " + effectString);
                    continue;
                }

                // Пробуем найти эффект по имени (поддерживаем оба формата: WEAKNESS и weakness)
                PotionEffectType effectType = null;
                try {
                    // Сначала пробуем через getByName (может быть устаревшим, но работает)
                    effectType = PotionEffectType.getByName(effectName.toUpperCase());
                    // Если не нашли, пробуем через все доступные типы
                    if (effectType == null) {
                        for (PotionEffectType type : PotionEffectType.values()) {
                            if (type != null && type.getName() != null &&
                                    type.getName().equalsIgnoreCase(effectName)) {
                                effectType = type;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Игнорируем
                }

                if (effectType == null) {
                    getLogger().warning("Неизвестный тип эффекта: " + effectName);
                    continue;
                }

                // В Minecraft уровни эффектов: 0 = I, 1 = II, 2 = III и т.д.
                // В конфиге пользователь указывает: 1 = I, 2 = II, 3 = III
                int effectLevel = Math.max(0, level - 1);
                // Используем продолжительность из конфига
                // ambient: false - для правильного визуального отображения уровня эффекта
                PotionEffect effect = new PotionEffect(effectType, duration, effectLevel, false, false);
                effects.add(effect);

            } catch (Exception e) {
                getLogger().warning("Ошибка при парсинге эффекта: " + e.getMessage());
            }
        }

        return effects;
    }

    /**
     * Запуск периодической проверки игроков
     */
    private void startPeriodicCheck() {
        if (checkTask != null) {
            checkTask.cancel();
        }

        // Сохраняем ссылку на плагин для безопасного использования в задаче
        // планировщика
        final DwarfModePlugin plugin = this;

        checkTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                plugin.checkAllPlayers();
            }
        }, checkInterval, checkInterval);
    }

    /**
     * Проверка всех онлайн игроков
     */
    private void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            try {
                checkPlayer(player);
            } catch (Exception e) {
                // Используем сохраненную ссылку на плагин для безопасного логирования
                getLogger().warning("Ошибка при проверке игрока " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Проверка одного игрока на применение эффектов
     */
    private void checkPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Location loc = player.getLocation();
        World world = player.getWorld();

        // Проверяем, разрешен ли этот мир для применения эффектов
        String worldName = world.getName();
        boolean worldEnabled = false;
        for (String enabledWorld : enabledWorlds) {
            if (worldName.equalsIgnoreCase(enabledWorld)) {
                worldEnabled = true;
                break;
            }
        }

        if (!worldEnabled) {
            return; // Пропускаем проверку для неразрешенных миров
        }

        int playerY = loc.getBlockY();
        UUID playerId = player.getUniqueId();

        // Обновляем кеш высоты (для оптимизации, но не пропускаем проверку эффектов)
        lastCheckedHeight.put(playerId, playerY);

        // 1. БОНУСЫ ПОД ЗЕМЛЕЙ
        // Применяем каждый checkInterval, независимо от изменения высоты
        if (undergroundBonusesEnabled && playerY <= undergroundMinDepth) {
            applyUndergroundBonuses(player, playerY);
        }

        // 2. СЛЕПОТА НА ОТКРЫТОМ НЕБЕ (всегда, не только днем)
        if (skyBlindnessEnabled) {
            checkSkyBlindness(player, world, loc);
        }

        // 3. ШТРАФЫ НА ПОВЕРХНОСТИ (применяем первыми)
        // Применяем каждый checkInterval
        if (surfacePenaltiesEnabled && playerY >= surfaceLevel) {
            applySurfacePenalties(player);
            if (surfaceDamageEnabled) {
                checkSurfaceDamage(player);
            }
        }

        // 4. ШТРАФЫ ЗА СОЛНЕЧНЫЙ СВЕТ (применяем последними, чтобы перезаписать
        // surfacePenalties)
        // Применяем каждый checkInterval
        if (sunlightPenaltiesEnabled) {
            checkSunlightPenalties(player, world, loc);
        }
    }

    /**
     * Применение бонусов под землей
     * Применяет эффекты накопительно для всех глубин, которые игрок достиг или
     * прошел
     * (чем глубже игрок, тем больше эффектов он получает)
     * 
     * Пример: если игрок на -35, а в конфиге есть эффекты для -10, -35, -45,
     * то применяются эффекты для -10 и -35 (все, что >= -35)
     * 
     * Важно: эффекты применяются от меньшей глубины к большей (от -10 к -45),
     * чтобы эффекты накапливались правильно
     */
    private void applyUndergroundBonuses(Player player, int playerY) {
        if (undergroundEffects.isEmpty()) {
            return;
        }

        try {
            // Применяем эффекты в порядке от меньшей глубины к большей (от -10 к -45)
            // Это важно для правильного накопления эффектов
            // Используем descendingMap() чтобы получить порядок от больших значений к
            // меньшим
            // Для отрицательных: -10 > -35 > -45, поэтому descendingMap даст -10, -35, -45
            for (Map.Entry<Integer, List<PotionEffect>> entry : undergroundEffects.descendingMap().entrySet()) {
                int depth = entry.getKey();
                // Применяем эффекты только если глубина больше или равна текущей глубине игрока
                // Для отрицательных: -10 >= -10 (true), -10 >= -35 (false), -35 >= -35 (true)
                // Это означает, что эффекты применяются накопительно
                if (depth >= playerY) {
                    for (PotionEffect effect : entry.getValue()) {
                        applyEffect(player, effect);
                    }
                }
            }

        } catch (Exception e) {
            getLogger().warning(
                    "Ошибка при применении бонусов под землей для игрока " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Проверка слепоты на открытом небе
     * Применяет слепоту, если игрок на открытом небе (над головой нет блоков)
     * Работает всегда, независимо от времени суток
     */
    private void checkSkyBlindness(Player player, World world, Location loc) {
        try {
            // Проверяем, что игрок на открытом небе
            int highestBlockY = world.getHighestBlockYAt(loc);
            if (loc.getBlockY() >= highestBlockY) {
                // Игрок на открытом небе - применяем слепоту
                applyEffect(player, new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            }
        } catch (Exception e) {
            getLogger().warning("Ошибка при проверке слепоты на открытом небе для игрока " + player.getName() + ": "
                    + e.getMessage());
        }
    }

    /**
     * Проверка штрафов за солнечный свет
     */
    private void checkSunlightPenalties(Player player, World world, Location loc) {
        try {
            // Проверяем, что игрок на открытом небе
            int highestBlockY = world.getHighestBlockYAt(loc);
            if (loc.getBlockY() < highestBlockY) {
                return; // Игрок не на открытом небе
            }

            // Проверяем время суток
            long time = world.getTime();
            boolean isDayTime = time >= dayTimeStart && time <= dayTimeEnd;

            if (!isDayTime) {
                return; // Ночь, солнца нет
            }

            // Проверяем, что игрок видит небо (нет блоков над головой)
            if (loc.getBlockY() >= highestBlockY) {
                // Применяем эффекты
                for (PotionEffect effect : sunlightEffects) {
                    applyEffect(player, effect);
                }

                // Поджигаем игрока, если включено
                if (sunlightBurn) {
                    player.setFireTicks(100);
                }
            }

        } catch (Exception e) {
            getLogger().warning(
                    "Ошибка при проверке солнечного света для игрока " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Применение штрафов на поверхности
     */
    private void applySurfacePenalties(Player player) {
        for (PotionEffect effect : surfaceEffects) {
            applyEffect(player, effect);
        }
    }

    /**
     * Проверка урона со временем на поверхности
     */
    private void checkSurfaceDamage(Player player) {
        UUID playerId = player.getUniqueId();
        Integer lastDamageTick = surfaceDamageTicks.get(playerId);

        if (lastDamageTick == null) {
            surfaceDamageTicks.put(playerId, 0);
            return;
        }

        // Увеличиваем счетчик на интервал проверки
        int newTick = lastDamageTick + checkInterval;

        // Если прошло достаточно времени, наносим урон
        if (newTick >= surfaceDamageInterval) {
            player.damage(surfaceDamage);
            surfaceDamageTicks.put(playerId, 0); // Сбрасываем счетчик
        } else {
            surfaceDamageTicks.put(playerId, newTick); // Сохраняем текущий счетчик
        }
    }

    /**
     * Применение эффекта игроку
     * Применяет эффект, перезаписывая существующий эффект того же типа
     * Важно: всегда перезаписывает, чтобы гарантировать правильное отображение
     * уровня
     */
    private void applyEffect(Player player, PotionEffect effect) {
        if (player == null || !player.isOnline() || effect == null) {
            return;
        }

        try {
            // Всегда удаляем существующий эффект перед применением нового
            // Это гарантирует правильное обновление уровня эффекта на клиенте
            if (player.hasPotionEffect(effect.getType())) {
                player.removePotionEffect(effect.getType());
            }

            // Применяем новый эффект
            // Используем addPotionEffect без параметра force (современный API)
            player.addPotionEffect(effect);
        } catch (Exception e) {
            getLogger().warning("Ошибка при применении эффекта " + effect.getType() + " игроку " + player.getName()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Обработчик события движения игрока
     * Удален - эффекты обновляются только по таймеру checkInterval
     */

    /**
     * Обработка команд
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("dwarfmode") || cmd.getName().equalsIgnoreCase("dwm")) {
            if (args.length == 0) {
                sender.sendMessage("§cНеверная команда!");
                sender.sendMessage("§e/dwarfmode reload - Перезагрузить конфигурацию");
                sender.sendMessage("§e/dwarfmode tear <amount> - Получить слезы гаста");
                sender.sendMessage("§e/dwarfmode info - Информация о плагине");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("dwm.reload") && !sender.isOp()) {
                    sender.sendMessage("§cУ вас нет прав на выполнение этой команды!");
                    return true;
                }

                try {
                    loadConfig();
                    lastCheckedHeight.clear();
                    surfaceDamageTicks.clear();
                    startPeriodicCheck();
                    sender.sendMessage("§aКонфигурация успешно перезагружена!");
                } catch (Exception e) {
                    sender.sendMessage("§cОшибка при перезагрузке конфига: " + e.getMessage());
                    getLogger().severe("Ошибка при перезагрузке конфига: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("info")) {
                sender.sendMessage("§6=== DwarfMode ===");
                sender.sendMessage("§eВерсия: " + getDescription().getVersion());
                sender.sendMessage("§eИнтервал проверки: " + (checkInterval / 20.0) + " сек");
                sender.sendMessage("§eРазрешенные миры: §b" + String.join(", ", enabledWorlds));
                sender.sendMessage("§eБонусы под землей: " + (undergroundBonusesEnabled ? "§aВключены" : "§cВыключены")
                        + " (" + undergroundEffects.size() + " уровней)");
                sender.sendMessage("§eСлепота на небе: " + (skyBlindnessEnabled ? "§aВключена" : "§cВыключена"));
                sender.sendMessage("§eШтрафы за солнце: " + (sunlightPenaltiesEnabled ? "§aВключены" : "§cВыключены"));
                sender.sendMessage(
                        "§eШтрафы на поверхности: " + (surfacePenaltiesEnabled ? "§aВключены" : "§cВыключены"));
                return true;
            }

            if (args[0].equalsIgnoreCase("tear")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cЭта команда может быть выполнена только игроком!");
                    return true;
                }
                if (!sender.hasPermission("dwm.tear") && !sender.isOp()) {
                    sender.sendMessage("§cУ вас нет прав на выполнение этой команды!");
                    return true;
                }

                int amount = 1;
                if (args.length > 1) {
                    try {
                        amount = Integer.parseInt(args[1]);
                        if (amount < 1)
                            amount = 1;
                        if (amount > 64)
                            amount = 64;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cНеверное количество! Используйте число от 1 до 64.");
                        return true;
                    }
                }

                Player p = (Player) sender;
                p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, amount));
                sender.sendMessage("§eВыдано " + amount + " слез(ы) гаста!");
                return true;
            }

            sender.sendMessage("§cНеизвестная команда!");
            sender.sendMessage("§e/dwarfmode reload - Перезагрузить конфигурацию");
            sender.sendMessage("§e/dwarfmode tear <amount> - Получить слезы гаста");
            sender.sendMessage("§e/dwarfmode info - Информация о плагине");
        }
        return true;
    }
}
