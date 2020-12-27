(ns user
  (:require [cheshire.core :as cheshire]
            [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [diabetes-dashboard-server.handler]
            [diabetes-dashboard-server.app :as app]
            [clj-http.client :as http]))
            


(def system nil) 

(defn init 
  []
  (alter-var-root #'system
                  (constantly (app/system))))

(defn start 
  []
  (alter-var-root #'system app/start))

(defn stop
  []
  (alter-var-root #'system 
                  (fn [s]
                    (let [{:keys [db server]} s]
                      (when (and db server) (app/stop s))))))

(defn go
  []
  (init)
  (start))


(defn container 
  "set refresh dirs when running in a container"
  []
  (set-refresh-dirs "/src/app/src"))

(defn reset
  []
  (stop)
  (refresh :after 'user/go))

(comment
  (clojure.tools.namespace.repl/clear)
  (start)
  (init)
  (stop)
  (reset)
  (go))

(def blood-sugars (slurp "./glucose-07-31-2020-09-30.json"))


(def evgs (:egvs (cheshire/parse-string blood-sugars true)))


; (http/post (get-in config [:dexcom :calibrations-endpoint]))

(def credentials (cheshire/parse-string "{\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IlR3eThiT1B4MGRvU1JoRk9WbGRnQlh0SkpiVSIsImtpZCI6IlR3eThiT1B4MGRvU1JoRk9WbGRnQlh0SkpiVSJ9.eyJpc3MiOiJodHRwczovL3VhbTEuZGV4Y29tLmNvbS9pZGVudGl0eSIsImF1ZCI6Imh0dHBzOi8vdWFtMS5kZXhjb20uY29tL2lkZW50aXR5L3Jlc291cmNlcyIsImV4cCI6MTYwODk2OTEwNiwibmJmIjoxNjA4OTYxOTA2LCJjbGllbnRfaWQiOiJGZXc2RkRzOHZHaFVOdGhSWUZQSVM3cmNvNHVWZ0EwZCIsInNjb3BlIjpbIm9mZmxpbmVfYWNjZXNzIiwiZWd2IiwiY2FsaWJyYXRpb24iLCJkZXZpY2UiLCJzdGF0aXN0aWNzIiwiZXZlbnQiXSwic3ViIjoiZWRmOGU5MDUtOTY0NC00MjAwLWIxZWUtMzllNzNjNjBjZjYxIiwiYXV0aF90aW1lIjoxNjA4OTYxODUwLCJpZHAiOiJpZHNydiIsImNvdW50cnlfY29kZSI6IlVTIiwianRpIjoiOTliMGZiNjQxZGIyZTQzMzI5YzYyNjlkYjYxZWJjNGEiLCJhbXIiOlsicGFzc3dvcmQiXX0.sOEQ3YFUV3KzLQOyJdZRpMrHXF2YAVQF7gRGMi-kiFc_Ed_YpYt1942WtK3iOMbkdfYENdb6w_si8knxmo46o8dhX3EgWFm2eDpIW9DPoiOtDuANt8sln8VpNt5XJpZfIjCR3VcClS-tD7rPXX9zefCrtvd_F9XTo2RZB2aND2qJYBp48Gfg3t8FZWNgU_upXzdW_8SO6kU7UgLYHdsW0WiKz1zuMusNnNxkVgq5_GrAmqHVVH8fv6T9eNnMphGAE3JrJMtXV5-NEiUQJFPtq6R_n0v41SDBfVl_0Wo7esNZgc6dl9M9-BNsqn5hTSX1l2IQMr26icZu3FnR1dL5oA\",\"expires_in\":7200,\"token_type\":\"Bearer\",\"refresh_token\":\"1d12cbb6339f5b83cac4360f3ec210d1\"}" true))
