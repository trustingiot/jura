define(
	["react", "js/firma/ValidateForm"],
	function( React, ValidateForm ) {
		return class Firma extends React.Component {
			constructor( props ) {
				super( props );

				this.createBlock = this.createBlock.bind( this );
			}

			createBlock( title, element ) {
				const key = title.replace( / /g, '' );
				return React.createElement( 'div', { key: key, className: 'firma-block' },
					React.createElement( element, { post: this.post } )
				);
			}

			post( service, data ) {
				var xhr = new XMLHttpRequest();
				xhr.open( 'POST', 'http://' + location.host + '/firma/' + service, true );
				xhr.setRequestHeader( 'Content-Type', 'application/json' );
				xhr.send( JSON.stringify( data ) );
			}

			render() {
				return React.createElement( 'div', { className: 'firma' },
					this.createBlock( 'Validate', ValidateForm )
				);
			}
		};
	} );