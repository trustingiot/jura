define( ["react", "js/anonymously/ValidateForm", "js/validate/Result"],
	function( React, ValidateForm, Result ) {
		return class Validate extends React.Component {
			constructor( props ) {
				super( props );

				this.state = { validationResult: [], processing: false, callback: null };

				this.createBlock = this.createBlock.bind( this );
				this.post = this.post.bind( this );
				this.updateResult = this.updateResult.bind( this );
			}

			createBlock( title, element, props = {} ) {
				const key = title.replace( / /g, '' );
				return React.createElement( 'div', { key: key, className: 'firma-block' },
					React.createElement( element, { ...{ post: this.post }, ...props } )
				);
			}

			post( service, data, callback ) {
				this.setState( { processing: true, validationResult: data, callback: callback } );
				var xhr = new XMLHttpRequest();
				xhr.onreadystatechange = function() {
					if ( xhr.readyState == XMLHttpRequest.DONE ) {
						this.setState( { processing: false } );
						if ( this.state.callback != null ) {
							this.state.callback( xhr.responseText );
						}
					}
				}.bind( this );
				xhr.open( 'POST', 'http://' + location.host + '/firma/' + service, true );
				xhr.setRequestHeader( 'Content-Type', 'application/json' );
				xhr.send( JSON.stringify( data ) );
			}

			updateResult( result ) {
				this.setState( { validationResult: JSON.parse( result ) } );
			}

			render() {
				return React.createElement( 'div', { className: 'firma' },
					this.createBlock( 'Validate', ValidateForm, { onValidateResult: this.updateResult } ),
					this.createBlock( 'Result', Result, { processing: this.state.processing, validationResult: this.state.validationResult } )
				);
			}
		};
	} );