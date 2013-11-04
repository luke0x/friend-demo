(ns ^{:name "Interactive form"
      :doc "Typical username/password authentication + logout + a pinch of authorization functionality"}
  cemerick.friend-demo.interactive-form
  (:require [cemerick.friend-demo.misc :as misc]
            [cemerick.friend-demo.users :as users :refer (users)]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [compojure.core :as compojure :refer (GET POST ANY defroutes)]
            (compojure [handler :as handler]
                       [route :as route])
            [ring.util.response :as resp]
            [hiccup.page :as h]
            [hiccup.element :as e]))

(def login-form
  [:div {:class "row"} [:div {:class "columns small-12"} [:div {:class "row"}
    [:form {:method "POST" :action "/interactive-form/login" :class "columns small-4"}
      [:div "Username" [:input {:type "text" :name "username"}]]
      [:div "Password" [:input {:type "password" :name "password"}]]
      [:div [:input {:type "submit" :class "button" :value "Login"}]]]]]])

(defn roles-or-anon
  [req]
  (if-let [identity (friend/identity req)]
    (apply str
      "Logged in, with these roles: " (-> identity friend/current-authentication))
    "anonymous user"))

(defn current-user
  []
  (friend/current-authentication))

(defn logged-in?
  []
  (if (current-user)
    true
    false))

(compojure/defroutes routes
  (GET "/" req
    (h/html5
      misc/pretty-head
      (misc/pretty-body
        (if (logged-in?)
          [:h3 "welcome, " (:fullname (current-user))]
          [:h3 "you should log in"])
        [:h4 "identity: " (str (friend/identity req))]
        [:h4 "current auth: " (str (friend/current-authentication))]
        [:p (roles-or-anon req)]
        (if (not (logged-in?))
          login-form)
        [:p (e/link-to (misc/context-uri req "requires-authentication") "Page requires authentication")]
        [:p (e/link-to (misc/context-uri req "logout") "Logout")])))
  (GET "/login" req
    (h/html5
      misc/pretty-head (misc/pretty-body login-form)))
  (GET "/logout" req
    (friend/logout*
      (resp/redirect (str (:context req) "/"))))
  (GET "/requires-authentication" req
    (friend/authenticated
      (h/html5
        misc/pretty-head
        (misc/pretty-body
          "Thanks for authenticating!")))))

(def page (handler/site
            (friend/authenticate
              routes
              {:allow-anon? true
               :login-uri "/login"
               :default-landing-uri "/"
               :unauthorized-handler #(-> (h/html5 [:h2 "You do not have sufficient privileges to access " (:uri %)])
                                        resp/response
                                        (resp/status 401))
               :credential-fn #(creds/bcrypt-credential-fn @users %)
               :workflows [(workflows/interactive-form)]})))