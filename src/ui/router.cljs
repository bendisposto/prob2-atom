(ns ui.router
  (:require-macros [reagent.ratom :as ra :refer [reaction]])
  (:require [cljs.nodejs :as node]
            [taoensso.sente  :as sente]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [cognitect.transit :as transit]
            [taoensso.encore :as enc  :refer (logf log logp)]))

(def prob (atom nil))
(def id (atom 0))
(defn fresh-id [] (swap! id inc))

(defn deep-merge [left right]
  (let [k (keys right)
        knew (remove #(contains? left %) k)
        kmerge (filter #(contains? left %) k)
        left' (reduce (fn [a e] (assoc a e (get right e))) left knew)]
    (reduce (fn [a e]
              (let [v (get left e)
                    v' (get right e)]
                (if (and (map? v) (map? v'))
                  (assoc a e (deep-merge v v'))
                  (if (= v v') a (assoc a e v'))))) left' kmerge)))

(defn read-transit [db msg]
  (if (:encoding db)
    (let [r (transit/reader (:encoding db))]
      (transit/read r msg))
    (keyword msg)))

(def relay
  (fn [{{send! :send!} :websocket :as db} [t m]]
    (send! [t m])
    db))

(defn decode
  "Takes a handler of 2 arguments, where the second argument is a vector of a
   message type and a message. The message is read through transit if the
   encoding has been set. Otherwise it create a keyword from the message
   (only used when fetching the encoding from the server).
   This middleware should be applied before the with-send middleware."
  [handler]
    (fn [db v]
      (handler db [(first v) (read-transit db (second v))])))

(defn with-send
  "Takes a handler of 2 arguments, where the second argument is a vector of a
   message type, a message and a function that sends a message to the server.
   Should be applied last!"
  [handler]
    (fn [{{send! :send!} :websocket :as db} [type msg]]
      (handler db [type msg send!])))

(rf/register-handler :chsk/encoding relay)

(rf/register-handler
 :sente/encoding
 (comp  rf/debug decode with-send)
 (fn [db [_ enc send!]]
   (send! [:prob2/handshake {}])
   (assoc db :encoding enc)))

(rf/register-handler
   :de.prob2.kernel/ui-state
   (comp rf/debug decode)
   (fn [db [_ deltas]]
     (deep-merge db deltas)))

(defn init-websocket []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! (str "127.0.0.1" ":" 3000 "/updates") {:type :auto})]
    (add-watch state :connection-observer (fn [_ _ _ new] (rf/dispatch [:connection-status (:open?  new)])))
    {:chsk chsk
     :ch-chsk ch-recv
     :send! send-fn
     :chsk-state state
     :stop! (sente/start-chsk-router!
             ch-recv
             (fn [e]
               (when (= (:id e) :chsk/recv)
                 (rf/dispatch (vec (:?data e))))))}))


(rf/register-sub
  :connected
  (fn [db _]
    (reaction (:connected @db))))

(rf/register-handler
  :connection-status
  rf/debug
  (fn [db [_ status]]
    (when status
     (rf/dispatch [:chsk/encoding]))
    (assoc db :connected status :encoding nil)))

(rf/register-handler
    :message
    rf/debug
    (fn [db [_ msg]] (logp :received msg) db))

(rf/register-handler
  :init
  rf/debug
  (fn [db _] (assoc db :websocket (init-websocket))))

(defn ^:extern subs-handler
    "Forwards changes to a subscription to a handler. First argument
    is the handler id that should be triggered. The second argument
    is a subscription pattern. All following arguments are added to
    the handler dispatch call. All arguments are preprocessed by the
    Clojurescript reader. The handler is called with the changed
    subscription. The arguments are also passed into this function.
    The registration of the handler must be done separately.

    For instance: We have a handler registered that uses the key :foo
    and we want to trigger :foo each time the trace with id
    09b2fdfa-f49b-4c5f-be64-ea5e63f0d628 changes.

    de.prob2.subs.subs_handler(':foo', '[:trace #uuid \"09b2fdfa-f49b-4c5f-be64-ea5e63f0d628\"])

    A matching handler could be registered with
    de.prob2.subs.register_handler(
    ':foo',
    function(x,db) { console.log('changed',x,
    'fulltree',db,
    'args',arguments);})
    "
    [handler hook & args]
    (let [real-handler (cljs.reader/read-string handler)
          real-hook (cljs.reader/read-string hook)
          x (rf/subscribe real-hook)]
      (ra/run! (rf/dispatch
                (into [real-handler
                       (clj->js @x)]
                      args)))))


(defn ^:extern register-handler
    "Registers a handler function. The first argument is the handler key,
    the second argument is a function of at least two arguments.
    The first argument is the changed part of the state. The second argument
    is the full state. Additional arguments are those that were passed from
    the subscription."
    [handler f]
    (let [real-handler (cljs.reader/read-string handler)]
      (rf/register-handler
       real-handler
       (fn [db [_ x & args]]
         (logp :handle real-handler)
         (let [jsdb (clj->js db)
               jsx (clj->js x)
               res (apply f jsx jsdb args)]
           db)))))

(defn listen [hook f & args]
  (let [handler (str "listener" (fresh-id))]
    (apply subs-handler handler hook args)
    (register-handler handler f)))


(defn ^:export start []
  (logp :atom js/atom)
  (logp :prob (.-prob js/atom))
  (reset! prob (.-prob js/atom))
  (set! (.-ui @prob) ui.router)
  (rf/dispatch [:init]))

(set! cljs.core/*main-cli-fn* start)
