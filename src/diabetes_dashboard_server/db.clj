(ns diabetes_dashboard_server.db
  (:require [next.jdbc :as next-jdbc]
            [cheshire.core :as cheshire]
            [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :refer [migrate rollback]]
            [yesql.core :refer [defquery]]))
            
(defonce db {:dbtype "sqlite" :dbname "dexcom-dev"})

(def migration-config
  {:datastore (ragtime-jdbc/sql-database {:dbtype "sqlite" 
                                          :dbname "dexcom-dev"}) 
   :migrations (ragtime-jdbc/load-resources "migrations")})

(defn init-db "Run all migrations on the database"
  []
  (println "Migrating")
  (migrate migration-config))

(defn rollback-db "Rollback the migrations on the database"
  []
  (rollback migration-config))

(defquery save-blood-sugar! 
  "sql/insert_blood_sugar.sql"
  {:connection db})

(defn save "Save a blood sugar record to the database"
  [record] 
  (save-blood-sugar! record))
