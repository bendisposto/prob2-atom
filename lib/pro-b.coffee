ProBView = require './pro-b-view'
StatusView = require './pro-b-status-view'
{CompositeDisposable} = require 'atom'

module.exports = ProB =
  proBView: null
  modalPanel: null
  subscriptions: null

  activate: (state) ->
    @proBView = new ProBView(state.proBViewState)
    @modalPanel = atom.workspace.addModalPanel(item: @proBView.getElement(), visible: false)

    # Events subscribed to in atom's system can be easily cleaned up with a CompositeDisposable
    @subscriptions = new CompositeDisposable

    # Register command that toggles this view
    @subscriptions.add atom.commands.add 'atom-workspace', 'pro-b:toggle': => @toggle()

  consumeStatusBar: (statusBar) ->
    @view = new StatusView()
    statusBar.addLeftTile(item: @view, priority: 200)

  deactivate: ->
    @modalPanel.destroy()
    @subscriptions.dispose()
    @proBView.destroy()

  serialize: ->
    proBViewState: @proBView.serialize()

  toggle: ->
    console.log 'ProB was toggled!'

    if @modalPanel.isVisible()
      @modalPanel.hide()
    else
      @modalPanel.show()
