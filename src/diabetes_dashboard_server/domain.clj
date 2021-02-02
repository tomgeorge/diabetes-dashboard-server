(ns diabetes-dashboard-server.domain
  (:require [diabetes-dashboard-server.db :as db])
  (:import [java.sql SQLException]))

(defn get-blood-sugar-counts
  "Get a count of blood sugar values grouped by value"
  []
  (db/get-blood-sugar-counts))

(defn get-blood-sugars
  "Get all blood sugar values in db"
  []
  (db/get-blood-sugars))

(defn import-range
  "import a collection of blood sugar values into the database"
  [blood-sugars]
  (try
    (db/save-blood-sugars! blood-sugars)
    (catch SQLException sql
      (ex-data sql))))

