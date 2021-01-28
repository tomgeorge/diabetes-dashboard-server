(ns diabetes-dashboard-server.db
  (:require [diabetes-dashboard-server.config :refer [config]]
            [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :refer [migrate rollback]]
            [yesql.core :refer [defquery]]
            [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))
            
(def blood_sugars_columns ["system_time" "display_time" "value" "realtime_value" "smoothed_value" "status" "trend" "trend_rate"])

(defn create-connections
  []
  {:database {:jdbc {:dbtype "sqlite" :dbname (get-in config [:database :name])}
              :ragtime {:datastore (ragtime-jdbc/sql-database {:dbtype "sqlite" :dbname (get-in config [:database :name])})
                        :migrations (ragtime-jdbc/load-resources "migrations")}}})

(defstate db :start (create-connections)
  :stop {})

(defn save-blood-sugars!
  "Save a list of records into blood_sugars"
  ([records db]
   (let [rows (map (comp vec vals) records)]
     (println (first rows)) 
     (sql/insert-multi! db "blood_sugars" blood_sugars_columns rows)))
  ([records]
   (save-blood-sugars! records (get-in db [:database :jdbc]))))

(defquery save-blood-sugar! 
  "sql/insert_blood_sugar.sql"
  {:connection (get-in db [:database :jdbc])})


(defquery count-blood-sugars
  "sql/count_blood_sugars.sql"
  {:connection (get-in db [:database :jdbc])})

(defquery get-blood-sugars
  "sql/get_blood_sugars.sql"
  {:connection (get-in db [:database :jdbc])})

(defn migrate-db "Run all migrations on the database"
  []
  (migrate (get-in db [:database :ragtime])))

(defn rollback-db "Rollback the migrations on the database"
  []
  (rollback (get-in db [:database :ragtime]))) 

(defn save 
  "Save a blood sugar record to the database"
  [record] 
  (save-blood-sugar! record))

(defn get-blood-sugar-counts
  "Get a map of blood sugar values to occurances of those values from the database"
  []
  (count-blood-sugars))

