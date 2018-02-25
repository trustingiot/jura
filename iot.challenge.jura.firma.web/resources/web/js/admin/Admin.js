define( ["react", "js/admin/DIW"],
	function( React, DIW ) {
		return class Admin extends React.Component {
			constructor( props ) {
				super( props );

				this.state = { diw: null, callback: null };

				this.createBlock = this.createBlock.bind( this );
				this.post = this.post.bind( this );
				this.updateDIW = this.updateDIW.bind( this );
			}

			createBlock( title, element, props = {} ) {
				const key = title.replace( / /g, '' );
				return React.createElement( 'div', { key: key, className: 'admin-block' },
					React.createElement( element, { ...{ post: this.post }, ...props } )
				);
			}

			post( service, data, callback ) {
				this.setState( { diw: null, callback: callback } );
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

			updateDIW( result ) {
				this.setState( { diw: JSON.parse( result ).diw } );
			}

			render() {
				return React.createElement( 'div', { className: 'admin' },
					this.createBlock( 'Device Identification Word', DIW, { diw: this.state.diw, onDIW: this.updateDIW } )
				);
			}
		};
	} );