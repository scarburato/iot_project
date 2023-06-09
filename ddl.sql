DROP DATABASE IF EXISTS smart_zoo;

CREATE DATABASE smart_zoo;

USE smart_zoo;

CREATE TABLE `float level` (
	`sensor's id`	INT NOT NULL,
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`low level`	BOOLEAN NOT NULL,
	
	PRIMARY KEY (`sensor's id`, `timestamp`)
);

CREATE TABLE `air quality` (
	`sensor's id`	INT NOT NULL,
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`concentration`	DECIMAL NOT NULL,
	
	PRIMARY KEY (`sensor's id`, `timestamp`)
);

CREATE TABLE `light intensity`(
	`sensor's id`	INT NOT NULL,
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`intensity`	DECIMAL NOT NULL,
	
	PRIMARY KEY (`sensor's id`, `timestamp`)
);

CREATE TABLE `humidity`(
	`sensor's id`	INT NOT NULL,
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`percentage`	DECIMAL NOT NULL,
	
	PRIMARY KEY (`sensor's id`, `timestamp`)
);

CREATE TABLE `temperature`(
	`sensor's id`	INT NOT NULL,
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`degrees`	DECIMAL NOT NULL,
	
	PRIMARY KEY (`sensor's id`, `timestamp`)
);

CREATE TABLE `actuator` (
    -- `id`        INT PRIMARY KEY,
    `ip`        INET6 PRIMARY KEY,
    `type`      VARCHAR(40) NOT NULL
);
