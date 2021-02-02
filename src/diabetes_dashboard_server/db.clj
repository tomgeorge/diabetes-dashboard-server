(ns diabetes-dashboard-server.db
  (:require [diabetes-dashboard-server.config :refer [config]]
            [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :refer [migrate rollback]]
            [yesql.core :refer [defquery]]
            [clojure.set :as set]
            [mount.core :refer [defstate]]
            [clojure.pprint :refer [pprint]]
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

(defn migrate-db "Run all migrations on the database"
  []
  (migrate (get-in db [:database :ragtime])))

(defn rollback-db "Rollback the migrations on the database"
  []
  (rollback (get-in db [:database :ragtime]))) 


(defquery find-by-time
  "sql/get_by_time.sql")
  
  
  

(defn record-difference
  "Return the elements in v1 that are not in v2"
  [v1 v2]
  (println "record-difference v1 " pprint v1)
  (println "record-difference v2 " pprint v2)
  (let [freqs (frequencies (concat v1 v2))]
    (reduce (fn [a e]
              (if (= 1 (val e))
                (conj a (key e))))
            []
            freqs)))


(defn save-blood-sugars!
  "Save a list of records into blood_sugars.  First checking if the records exist to avoid primary key collissions"
  ([records db & {:keys [db-spec]}]
   (let [rows (map (comp vec vals) records)
         system-times (map :systemTime records)
         existing (find-by-time {:system_times system-times} {:connection db-spec})
         not-yet-inserted (record-difference records existing)]
     (println "inserting the following " (count not-yet-inserted) " records " not-yet-inserted)
     (sql/insert-multi! db "blood_sugars" blood_sugars_columns not-yet-inserted)))
  ([records]
   (save-blood-sugars! records (get-in db [:database :jdbc]))))

(defquery count-blood-sugars
  "sql/count_blood_sugars.sql"
  {:connection (get-in db [:database :jdbc])})

(defn get-blood-sugar-counts
  "Get a map of blood sugar values to occurances of those values from the database"
  []
  (count-blood-sugars))

(defquery save-blood-sugar! 
  "sql/insert_blood_sugar.sql"
  {:connection (get-in db [:database :jdbc])})

(defn save 
  "Save a blood sugar record to the database"
  [record] 
  (save-blood-sugar! record))

(defquery get-blood-sugars
  "sql/get_blood_sugars.sql"
  {:connection (get-in db [:database :jdbc])})

