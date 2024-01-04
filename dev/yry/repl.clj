(ns yry.repl)

(comment 
  
  "Portal"
  
  (require '[portal.api :as p])
  (def p (p/open {:launcher :vs-code}))
  
  (add-tap #'p/submit)
  
  (tap> 1)
  )