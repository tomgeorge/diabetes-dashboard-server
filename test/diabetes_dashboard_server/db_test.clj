(ns diabetes-dashboard-server.db-test 
  (:require
    [diabetes-dashboard-server.db :as db]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [mount.core :refer [defstate] :as mount]
    [cprop.core :refer [load-config]]
    [next.jdbc :as next-jdbc]
    [next.jdbc.sql :as sql]
    [cheshire.core :as cheshire]
    [integrant.core :as integrant]
    [ragtime.jdbc :as ragtime-jdbc]
    [yesql.core :refer [defquery]]
    [ragtime.repl :refer [migrate]]))

(def conf 
  {:diabetes-dashboard-server/configuration {:database {:dbtype "sqlite" :dbname ":memory:"}}})

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



(let [datasource (next-jdbc/get-datasource {:dbtype "sqlite" :dbname "testdb"}) 
      blood-sugars (take 20 (json->blood-sugars "resources/dev/glucose-07-31-2020-09-30.json"))
      system-times (map :systemTime (take 10 blood-sugars))]
  (with-open [conn (next-jdbc/get-connection {:dbtype "sqlite" :dbname "testdb"})]
    (db/find-by-time {:system_times system-times} {:connection conn})))

(with-open [connection (next-jdbc/get-connection {:dbtype "sqlite" :dbname "testdb"})]
  (next-jdbc/execute! connection ["select * from blood_sugars"]))



(deftest ignore-existing-records
  (testing "when inserting blood sugar records, should ignore records that are already in the database"
    (let [db-spec {:dbtype "sqlite" :dbname "testdb"}
          datasource (next-jdbc/get-datasource db-spec) 
          conn (next-jdbc/get-connection datasource)
          ragtime {:datastore (ragtime-jdbc/sql-database {:connection conn})
                   :migrations (ragtime-jdbc/load-resources "migrations")}
          blood-sugars (take 20 (json->blood-sugars "resources/dev/glucose-07-31-2020-09-30.json"))]
      (migrate ragtime)
      (db/save-blood-sugars! (take 10 blood-sugars) conn :db-spec db-spec)
      (db/save-blood-sugars! (drop 9 blood-sugars) conn :db-spec db-spec)
      (is (= 19 (next-jdbc/execute! conn ["select * from blood_sugars"]))))))
