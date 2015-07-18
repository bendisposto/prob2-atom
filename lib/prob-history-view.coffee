{$, $$$, View}  = require 'atom-space-pen-views'

module.exports =
class HistoryView extends View

  @content: (params) ->
    @div =>
      @h1 =>
        @span "Connection Status: "
        @span outlet: "connected"

  initialize: (params) ->
    atom.prob.ui.listen('[:connected]',@changeConnection)
    @connected.html("Not connected")

  changeConnection:(x,db) =>
    if x then this.connected.html("Connected") else this.connected.html("Not Connected")

  getTitle: ->
    "ProB"
