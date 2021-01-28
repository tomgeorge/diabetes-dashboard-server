;; TODO
;; + specify explicit exceptions to catch (SQLException, clj-http exception)
;;     - what exception is clj-http throwing when I get a non-200 status?
;; + the domain/db functions should ignore primary key exceptions when inserting 
;;   into blood_sugars. If there is already that systemtime record there, move on 
;;
;;
;;

(ns diabetes-dashboard-server.handler
  (:require [compojure.core :refer [defroutes context GET POST]]
            [compojure.route :as route]
            [diabetes-dashboard-server.config :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring-debug-logging.core :refer [wrap-with-logger]]
            [clojure.string :as string]
            [slingshot.slingshot :refer [try+]]
            [diabetes-dashboard-server.domain :refer [get-blood-sugar-counts get-blood-sugars] :as domain]
            [io.pedestal.log :as log]
            [clj-http.client :as http]
            [cheshire.core :as cheshire]))


(def login-endpoint (get-in config [:dexcom :login-endpoint]))

(defn- login [client-id redirect-uri]
  (let [response (http/get (str login-endpoint
                              "?client_id=" client-id 
                              "&redirect_uri=" redirect-uri 
                              "&response_type=code&scope=offline_access"))]
    response))

(defn- handle-login [request]
  (let [{{:keys [client_id redirect_uri]} :body} request]
    (log/info "handle-login" nil)
    (login client_id redirect_uri)))

(defn- request-access-token [response-code]
  (println "response code is " response-code)
  (try
    (let [oauth-endpoint (get-in config [:dexcom :oauth-endpoint])
          client-secret (get-in config [:dexcom :client-secret])
          client-id (get-in config [:dexcom :client-id])
          redirect-uri (get-in config [:dexcom :redirect-uri])
          response (http/post oauth-endpoint {:debug true
                                              :debug-body true
                                              :headers {"cache-control" "no-cache"}
                                              :form-params {:client_secret client-secret
                                                            :client_id client-id
                                                            :code response-code
                                                            :grant_type "authorization_code"
                                                            :redirect_uri redirect-uri}})]
      response)
    (catch Exception e
      (let [{:keys [status body]} (ex-data e)]
        {:status status :body body}))))


(defn- get-dexcom-blood-sugars
  "Get a count of blood sugar values between a start and end date from the 
  dexcom endpoint"
  [request]
  (try
    (let [{{:keys [start_date end_date]} :params} request
          authorization (get-in request [:headers "authorization"])
          blood-sugar-endpoint (get-in config [:dexcom :blood-sugar-endpoint])
          response (http/get blood-sugar-endpoint {:debug true
                                                   :headers {:authorization authorization}
                                                   :query-params {:startDate start_date
                                                                  :endDate end_date}})
          body (:body response)
          egvcount (count (:egvs (cheshire/parse-string body true)))]
      {:status 200 :body {:count egvcount}})
    (catch Exception e
      (let [edata (ex-data e)
            {:keys [status body]} edata]
        {:status status :body body}))))

(defn- import-range 
  "Given a start and end date, request the data from dexcom and save it to the database"
  [authorization start-date end-date]
  (try
    (let [blood-sugar-endpoint (get-in config [:dexcom :blood-sugar-endpoint])
          response (http/get blood-sugar-endpoint {:debug true
                                                   :headers {:authorization authorization}
                                                   :query-params {:startDate start-date
                                                                  :endDate end-date}})
          {:keys [body]} response
          blood-sugars (:egvs (cheshire/parse-string body true))]
      (log/info "hi" "mom")
      (domain/import-range blood-sugars))
    (catch Exception e
      (log/info "exception-handler-import-range" (ex-data e))
      (let [edata (ex-data e)
            {:keys [status body]} edata]
        {:status status :body body}))))
      

(defn- handle-request-access-token [request]
  (log/info :request request) 
  (println "handle-request-access-token request " request)
  (let [{{:keys [responseCode]} :body} request]
      (request-access-token responseCode)))
     
    

(defn- handle-get-blood-sugar-counts
  []
  (get-blood-sugar-counts))

(defn- handle-get-blood-sugars
  []
  (get-blood-sugars))

(defn- handle-get-dexcom-blood-sugars
  [request]
  (get-dexcom-blood-sugars request))

(defn- handle-import-range
  [request]
  (let [{{:keys [start_date end_date]} :body} request
        authorization (get-in request [:headers "authorization"])]
    (import-range authorization start_date end_date)))

;; TODO: destructure stuff out of the request here
(defroutes app-routes
  (context "/api" []
    (GET "/" [] "hello")
    (GET "/blood-sugar-counts" [] (handle-get-blood-sugar-counts))
    (GET "/blood-sugars" [] (handle-get-blood-sugars))
    (POST "/login" request (handle-login request))
    (POST "/request-access-token" request (handle-request-access-token request))
    (context "/dexcom" []
      (GET "/blood-sugars" request (handle-get-dexcom-blood-sugars request))
      (POST "/import-range" request (handle-import-range request))))
     
  (route/not-found "Not Found"))

(def handler
  (-> app-routes
      (wrap-json-response)
      (wrap-params)
      (wrap-keyword-params)
      (wrap-json-body {:keywords? true})
      (wrap-defaults api-defaults)))
      
