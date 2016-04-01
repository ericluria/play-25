# --- !Ups

CREATE TABLE IF NOT EXISTS `people` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

# --- !Downs

DROP TABLE IF EXISTS `people`;