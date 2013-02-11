CREATE TABLE IF NOT EXISTS `english` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `word` VARCHAR(1000) NOT NULL,
    `rating` INTEGER default 0 NOT NULL,
    `created` TIMESTAMP default (DATETIME('now', 'localtime')) NOT NULL,
    `updated` TIMESTAMP default (DATETIME('now', 'localtime')) NOT NULL,
     UNIQUE(word)
);

CREATE TABLE IF NOT EXISTS `russian` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `word` VARCHAR(1000) NOT NULL,
    `rating` INTEGER default 0 NOT NULL,
    `created` TIMESTAMP default (DATETIME('now', 'localtime')) NOT NULL,
    `updated` TIMESTAMP default (DATETIME('now', 'localtime')) NOT NULL,
     UNIQUE(word)
);

CREATE TABLE IF NOT EXISTS `english_russian` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     russian_id INTEGER NOT NULL,
     english_id INTEGER NOT NULL,
    `created` TIMESTAMP default (DATETIME('now', 'localtime')) NOT NULL,
     FOREIGN KEY(russian_id) REFERENCES russian(_id),
     FOREIGN KEY(english_id) REFERENCES english(_id),
     UNIQUE(russian_id, english_id)
);
