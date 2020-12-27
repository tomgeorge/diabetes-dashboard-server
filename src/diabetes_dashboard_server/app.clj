(ns diabetes-dashboard-server.app
  (:gen-class)
  (:require [diabetes-dashboard-server.handler :refer [app]]
            [diabetes-dashboard-server.config :refer [init-config! unset-config!]]
            [ring.adapter.jetty9 :refer [run-jetty stop-server]]
            [diabetes_dashboard_server.db :refer [init-db rollback-db]]))
            
(defn system "Return a whole instance of the application"
  []
  {:db nil
   :server nil}) 

(defn start "Load config, create database resource, start http server"
  [system]
  (let [config (init-config!)
        db {:dbtype "sqlite" :dbname (get-in config [:database :name])}
        _ (init-db)]
    (-> system
        (assoc :db db)
        (assoc :server (run-jetty app {:port (get-in config [:server :port])
                                       :join? false})))))

(defn stop "Unload config, unset db, stop server"
  [system]
  (let [{:keys [server]} system]
    (stop-server server)
    (unset-config!)
    (rollback-db)
    ()
    (-> system
        (assoc :db nil)
        (assoc :server nil))))

(defn -main [& _]
  (start (system)))
