(ns cemerick.friend-demo.users
  (:require [cemerick.friend.credentials :refer (hash-bcrypt)]))

(def users (atom {"user" {:username "user"
                          :fullname "Default User"
                          :password (hash-bcrypt "password")
                          :roles #{::user}}
                  "friend" {:username "friend"
                            :password (hash-bcrypt "clojure")
                            :roles #{::user}}}))