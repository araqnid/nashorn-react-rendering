define(['react', 'jsx!Component'], function(React, Component) {
  var AggregateComponent = React.createClass({
    render: function() {
      return <ul>
               <li><Component /></li>
               <li><Component /></li>
             </ul>;
    }
  });
  return AggregateComponent;
})
