(ns diabetes-dashboard-server.db-test 
  (:require
    [diabetes-dashboard-server.db :as db]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [mount.core :refer [defstate] :as mount]
    [cprop.core :refer [load-config]]
    [next.jdbc :as next-jdbc]
    [cheshire.core :as cheshire]
    [ragtime.jdbc :as ragtime-jdbc]
    [ragtime.repl :refer [migrate]]))

(defn json->blood-sugars
  [json]
  (let [file (slurp json)]
    (-> file
        (cheshire/parse-string true)
        :egvs)))

(defn db-fixture 
  [f]
  (let [conns (db/create-connections)])
  (mount/start-with-states { #'diabetes-dashboard-server.config/config {:start #(load-config :file "resources/test/config.edn")}
                             #'diabetes-dashboard-server.db/db {:start #(db/create-connections)}})
  (f)
  (mount/stop))

(let [datasource (next-jdbc/get-datasource {:dbtype "sqlite" :dbname ":memory:"}) 
      conn (next-jdbc/get-connection datasource)
      ragtime {:datastore (ragtime-jdbc/sql-database {:connection  conn})
               :migrations (ragtime-jdbc/load-resources "migrations")}]
  (migrate ragtime)
  (next-jdbc/execute! conn ["select * from ragtime_migrations"])
  (db/save-blood-sugars! (take 10 (json->blood-sugars "resources/dev/glucose-07-31-2020-09-30.json")) conn)
  (count (next-jdbc/execute! conn ["select * from blood_sugars"])))

(deftest ignore-existing-records
  (testing "when inserting blood sugar records, should ignore records that are already in the database"
    (let [datasource (next-jdbc/get-datasource {:dbtype "sqlite" :dbname ":memory:"}) 
          conn (next-jdbc/get-connection datasource)
          ragtime {:datastore (ragtime-jdbc/sql-database {:connection conn})
                   :migrations (ragtime-jdbc/load-resources "migrations")}
          blood-sugars (take 20 (json->blood-sugars "resources/dev/glucose-07-31-2020-09-30.json"))]
      (migrate ragtime)
      (db/save-blood-sugars! (take 10 blood-sugars) conn)
      (db/save-blood-sugars! (drop 9 blood-sugars) conn)
      (println (next-jdbc/execute! conn ["select * from ragtime_migrations"]))
      (is (= 19 (count (next-jdbc/execute! conn ["select * from blood_sugars"])))))))



 
(comment
  {:database {:jdbc {:dbtype "sqlite" :dbname (get-in config [:database :name])}
              :ragtime {:datastore (ragtime-jdbc/sql-database {:dbtype "sqlite" :dbname (get-in config [:database :name])})
                        :migrations (ragtime-jdbc/load-resources "migrations")}}})
