-- SCHEMA DATABASE PROGETTO "NASA UNIVERSE EXPLORER"
-- Il sistema utilizza 3 tabelle principali per la persistenza dei dati.

-- 1. Tabella Asteroidi (Dati NeoWs)
CREATE TABLE asteroids (
                           id VARCHAR(255) NOT NULL PRIMARY KEY,
                           name VARCHAR(255),
                           diameter_meters FLOAT,
                           is_hazardous BOOLEAN,
                           close_approach_date DATE,
                           velocity_kmh FLOAT,
                           miss_distance_km FLOAT
);

-- 2. Tabella Eventi Terrestri (Dati EONET)
CREATE TABLE earth_events (
                              id VARCHAR(255) NOT NULL PRIMARY KEY,
                              title VARCHAR(255),
                              category VARCHAR(100),
                              date DATE,
                              latitude FLOAT,
                              longitude FLOAT
);

-- 3. Tabella Foto Rover Marte (Dati Mars Rover Photos)
CREATE TABLE mars_photos (
                             id BIGINT NOT NULL PRIMARY KEY,
                             sol INTEGER,
                             img_src TEXT, -- URL dell'immagine
                             camera_name VARCHAR(50),
                             earth_date DATE
);

-- Nota: Gli Esopianeti vengono gestiti come Live Feed e non persistiti per scelta architetturale (ottimizzazione storage).