{$, $$$, View}  = require 'atom-space-pen-views'

module.exports =
class HistoryView extends View

  @content: (params) ->
    @div =>
      @h1 "Connection Status: "
      @h2 outlet: "connected"

  initialize: (params) ->
    atom.prob.ui.subs_handler(':connection', '[:connected]')
    atom.prob.ui.register_handler(':connection', @changeConnection)

  changeConnection:(x,db) =>
    console.log this
    #@connected.html(x)

  sayHello: ->
    @personalGreeting.html("#{@greeting}, #{@name.val()}")

  getTitle: ->
    "ProB"
