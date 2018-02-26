define( ["react"], function( React ) {
	return class Form extends React.Component {
		constructor( props ) {
			super( props );

			this.submitForm = this.submitForm.bind( this );
		}

		renderInput( id, type, placeholder ) {
			return React.createElement( 'div', { key: id, className: 'form-group table-row' },
				React.createElement( 'input', { key: id, type: type, className: 'form-control', id: id, placeholder: placeholder, ref: ( v ) => this[id] = v } )
			);
		}

		submitForm() {
			let device = this.device.value.trim();
			let diw = this.diw.value.trim();
			if ( device != '' && diw != '' ) {
				this.props.post( 'transactions', {
					device: device,
					diw: diw,
				}, this.props.onResult );
			}
		}

		handleSubmit( event ) {
			event.preventDefault();
		}

		render() {
			return React.createElement( 'div', { className: 'container submit-form' },
				React.createElement( 'form', { onSubmit: this.handleSubmit },
					this.renderInput( 'device', 'text', 'Device MAC' ),
					this.renderInput( 'diw', 'password', 'Device Identification Word' ),
					React.createElement( 'button', { type: 'submit', className: 'btn btn-dark btn-block', onClick: this.submitForm }, 'Obtain transactions' )
				)
			);

		}
	};
} );