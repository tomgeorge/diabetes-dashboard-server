(ns diabetes-dashboard-server.handler
  (:require [compojure.core :refer [defroutes context GET POST]]
            [compojure.route :as route]
            [diabetes-dashboard-server.config :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring-debug-logging.core :refer [wrap-with-logger]]
            [clj-http.client :as http]))


(def client-secret (get-in @config [:dexcom :client-secret]))
(def client-id (get-in @config [:dexcom :client-id]))
(def redirect-uri (get-in @config [:dexcom :redirect-uri]))
(def oauth-endpoint (get-in @config [:dexcom :oauth-endpoint])) 
(def login-endpoint (get-in @config [:dexcom :login-endpoint]))

(defn login [client-id redirect-uri]
  (let [response (http/get (str login-endpoint
                              "?client_id=" client-id 
                              "&redirect_uri=" redirect-uri 
                              "&response_type=code&scope=offline_access"))]
    (println response)
    response))

(defn handle-login [request]
  (let [{{:keys [client_id redirect_uri]} :body} request]
    (login client_id redirect_uri)))
  
(defn request-access-token [response-code]
  (http/post oauth-endpoint {:debug true
                             :debug-body true
                             :headers {"cache-control" "no-cache"}
                             :form-params {:client_secret client-secret
                                           :client_id client-id
                                           :code response-code
                                           :grant_type "authorization_code"
                                           :redirect_uri redirect-uri}}))

(defn handle-request-access-token [request]
  (let [{{:keys [responseCode]} :body} request]
    (request-access-token responseCode)))

(defroutes app-routes
  (context "/api" []
    (GET "/" [] "hello")
    (POST "/login" request (handle-login request))
    (POST "/request-access-token" request (handle-request-access-token request)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-json-body {:keywords? true})
      (wrap-defaults api-defaults)
      (wrap-with-logger)))
