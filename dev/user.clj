(ns user
  (:require [cheshire.core :as cheshire]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [diabetes-dashboard-server.app :as app]
            [diabetes-dashboard-server.db :as db]
            [ring.adapter.jetty9 :refer [stop-server]]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [next.jdbc :as next-jdbc]
            [diabetes-dashboard-server.config :refer [config]]
            [next.jdbc.prepare :as p]
            [next.jdbc.sql :as sql]
            [next.jdbc.sql.builder :as builder]
            [mount.core :as mount]
            [io.pedestal.log :as log]
            [clojure.java.classpath :as cp]))


(defn start
  []
  (log/info "starting components" nil)
  (mount/start #'diabetes-dashboard-server.config/config
               #'diabetes-dashboard-server.db/db
               #'diabetes-dashboard-server.app/app))

(defn stop
  []
  (mount/stop))

(defn go
  []
  (start)
  :ready)



(defn reset
  []
  (stop)
  (refresh :after 'user/go))


(apply + '( 1 2 3 4 5))


(filter odd? (map inc [1 2 3 4 5]))

(defn seed-blood-sugar-data
  [path]
  (let [blood-sugars (slurp path)
        egvs (:egvs (cheshire/parse-string blood-sugars true))
        egvcount (count egvs)]
    (println "found " egvcount "records")
    (db/save-blood-sugars! egvs)))

(defn load-test-data
  []
  (do
    (seed-blood-sugar-data "./resources/dev/glucose-10-01-2020-12-21-2020.json")
    (seed-blood-sugar-data "./resources/dev/glucose-07-31-2020-09-30.json")))

(defn container 
  "set refresh dirs when running in a container"
  []
  (set-refresh-dirs "/src/app/src" "/src/app/test"))

(defn unbind-stuff
  "When you get a weird repl thing going on this will unbind the aliases in user"
  []
  (ns-unalias *ns* 'app)
  (ns-unalias *ns* 'db)
  (ns-unalias *ns* 'config))

(comment
  (clojure.tools.namespace.repl/clear)

  config
  (db/migrate-db)
  (db/rollback-db)
  (start)
  (stop)
  (refresh)
  (reset)
  (mount/find-all-states)
  (container)
  (go))
  
(comment
 (cp/classpath)
 (db/get-blood-sugar-counts)
 (next-jdbc/execute! (get-in db/db [:database :jdbc]) ["select sql from sqlite_master where name='blood_sugars'"])
 (count (next-jdbc/execute! (get-in db/db [:database :jdbc]) ["select * from blood_sugars where system_time > datetime('2020-09-30T00:00:00')"]))
 (next-jdbc/execute! (get-in db/db [:database :jdbc]) ["select value, count(value) as count from blood_sugars group by value"])
 (next-jdbc/execute! (get-in db/db [:database :jdbc]) ["select count(*) from blood_sugars "])
 (db/get-blood-sugar-counts))
