define( ["react"], function( React ) {
	return class ScannerOutput extends React.Component {
		constructor( props ) {
			super( props );
		}

		formatEvent( e ) {
			let date = new Date( e.timestamp );
			let formattedDate = date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
			let beacon = e.protocol + ' ' + e.address;
			let distance = '[rssi:' + e.rssi + ']';
			let scanner = e.topic.split( "/" )[4];

			return formattedDate + ' ==> ' + beacon + ' detected ' + distance + ' from ' + scanner;
		}

		render() {
			let socket = this.props.socket;
			let title = ( socket == null ) ? 'No active connection' : 'Connected to ' + socket.url;
			const events = this.props.events
				.map(( e ) => JSON.parse( e.data ) )
				.map(( e ) => [e.timestamp, this.formatEvent( e )] ).sort( function( a, b ) { return b[0] - a[0] } )
				.map(( e ) => React.createElement( 'p', { className: 'card-text', key: e[0] }, e[1] )
				);
			return React.createElement( 'div', { className: 'card' },
				React.createElement( 'div', { className: 'card-header' }, title ),
				React.createElement( 'div', { className: 'card-block' }, events )
			);
		}
	};
} );