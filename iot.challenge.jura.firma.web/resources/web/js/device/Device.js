define( ["react", "js/device/Form", "js/device/Result"],
	function( React, Form, Result ) {
		return class Device extends React.Component {
			constructor( props ) {
				super( props );

				this.state = { device: null, transactions: [], reject: null, processing: false, callback: null };

				this.createBlock = this.createBlock.bind( this );
				this.post = this.post.bind( this );
				this.updateTransactions = this.updateTransactions.bind( this );
			}

			createBlock( title, element, props = {} ) {
				const key = title.replace( / /g, '' );
				return React.createElement( 'div', { key: key, className: 'device-block' },
					React.createElement( element, { ...{ post: this.post }, ...props } )
				);
			}

			post( service, data, callback ) {
				this.setState( { processing: true, reject: null, device: data.device, transactions: [], callback: callback } );
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

			updateTransactions( result ) {
				var response = JSON.parse( result );
				this.setState( {
					reject: response.reject,
					transactions: response.transactions.reverse(),
					device: response.device
				} );
			}

			render() {
				return React.createElement( 'div', { className: 'device' },
					this.createBlock( 'Transactions form', Form, { onResult: this.updateTransactions } ),
					this.createBlock( 'Transactions', Result, { processing: this.state.processing, device: this.state.device, transactions: this.state.transactions, reject: this.state.reject } )
				);
			}
		};
	} );