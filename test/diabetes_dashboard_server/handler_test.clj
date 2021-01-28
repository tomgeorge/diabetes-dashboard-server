(ns diabetes-dashboard-server.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [mount.core :as mount]
            [cprop.core :refer [load-config]]
            [clj-http.fake :as fake]
            [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [diabetes-dashboard-server.handler :as handler]))

(defn mount-fixture
  "Swap out the db component started by mount with a data structure"
  [f]
  (let [test-config (load-config :resource "config_test.edn")]
    (mount/start-with {#'diabetes-dashboard-server.config/config test-config})
    (f)
    (mount/stop)))

(use-fixtures :each mount-fixture)
  
(deftest test-app
  (testing "main route"
    (let [response (handler/handler (mock/request :get "/api"))]
      (is (= (:status response) 200))
      (is (= (:body response) "hello"))))

  (testing "not-found route"
    (let [response (handler/handler (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest test-request-acress-token
  (testing "a bad access code"
    (with-redefs [http/post (fn [_ _] (throw (ex-info "http exception" {:status 400 :body {:error "invalid grant"}})))]
      (let [response (handler/handler (-> (mock/request :post "/api/request-access-token")
                                          (mock/json-body {:responseCode "code"})))]
        (is (= 400 (:status response)))
        (is (= (cheshire/generate-string {:error "invalid grant"}) (:body response)))))))
