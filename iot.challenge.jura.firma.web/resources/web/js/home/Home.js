define( ["react"], function( React ) {
	return class Home extends React.Component {
		constructor( props ) {
			super( props );
		}

		render() {
			return React.createElement( 'h1', null, this.props.alias );
		}
	};
} );