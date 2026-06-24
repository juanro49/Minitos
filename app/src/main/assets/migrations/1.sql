CREATE TABLE IF NOT EXISTS `vehicles` (`id` TEXT NOT NULL, `name` TEXT, `last_update` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `network_id` TEXT, `address` TEXT, `extra_data` TEXT, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `zones` (`id` INTEGER NOT NULL, `fill_color` TEXT, `color` TEXT, `width` INTEGER NOT NULL, `points` TEXT, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `fav_vehicles` (`id` TEXT NOT NULL, PRIMARY KEY(`id`));
