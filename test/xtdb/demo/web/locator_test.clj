;; Copyright © 2024, JUXT LTD.

(ns xtdb.demo.web.locator-test
  (:require
   [xtdb.demo.web.locator :as locator]
   [clojure.test :refer [deftest is]]))

(deftest find-resource-test
  (let [resource-tree
        [{:web-context "/"
          :resources [{:web-path "customers"
                       :resource-fn (fn [_] {:id :customers})}]}]]
    (is (= {:id :customers} (locator/find-resource resource-tree "/customers")))))
