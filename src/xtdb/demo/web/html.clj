(ns xtdb.demo.web.html)

(defn html-table [rows {:keys [rowspecs]}]
  [:table
   (for [rowspec rowspecs]
     (cond
       (keyword? rowspec) [:th (str rowspec)]))
   (for [row rows]
     [:tr
      (for [rowspec rowspecs]
        (cond
          (keyword? rowspec) [:td (get row rowspec)]))])])
