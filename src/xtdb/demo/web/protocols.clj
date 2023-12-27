(ns xtdb.demo.web.protocols)

(defprotocol UniformInterface
  (allowed-methods [_ request])
  (GET [_ request])
  (HEAD [_ request])
  (POST [_ request])
  (PUT [_ request])
  (DELETE [_ request])
  (OPTIONS [_ request]))
