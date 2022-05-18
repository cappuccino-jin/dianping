CREATE TABLE `dianping`.`user` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `created_at` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
    `update_at` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
    `telphone` varchar(40) NOT NULL DEFAULT '',
    `password` varchar(200) NOT NULL DEFAULT '',
    `nick_name` varchar(40) NOT NULL DEFAULT '',
    `gender` int(11) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `telphone_unique_index` (`telphone`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;