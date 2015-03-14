define(['react', 'jsx!Component'], function(React, Component) {
  var AggregateComponent = React.createClass({
    render: function() {
      return <ul>
               <li><Component name="Component1" /></li>
               <li><Component name="Component2" /></li>
             </ul>;
    }
  });
  return AggregateComponent;
})
