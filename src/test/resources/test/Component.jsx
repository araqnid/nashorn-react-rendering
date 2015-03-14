define(['react'], function(React) {
  var Component = React.createClass({
    propTypes: {
      name: React.PropTypes.string.isRequired
    },
    getDefaultProps: function() {
      return {
        name: "Component"
      };
    },
    render: function() {
      return <div>{ this.props.name } content</div>;
    }
  });
  return Component;
})
