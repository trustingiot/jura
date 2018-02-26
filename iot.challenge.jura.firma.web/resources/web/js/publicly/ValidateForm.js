define( ["react"], function( React ) {
	return class ValidateForm extends React.Component {
		constructor( props ) {
			super( props );

			this.submitValidation = this.submitValidation.bind( this );
		}

		renderInput( id, type, placeholder ) {
			return React.createElement( 'div', { key: id, className: 'form-group table-row' },
				React.createElement( 'input', { key: id, type: type, className: 'form-control', id: id, placeholder: placeholder, ref: ( v ) => this[id] = v } )
			);
		}

		submitValidation() {
			this.props.post( 'validate', {
				transaction: this.transaction.value,
				mode: 'publicly'
			}, this.props.onValidateResult );
		}

		handleSubmit( event ) {
			event.preventDefault();
		}

		render() {
			return React.createElement( 'div', { className: 'container submit-form' },
				React.createElement( 'form', { onSubmit: this.handleSubmit },
					this.renderInput( 'transaction', 'text', 'Transaction id' ),
					React.createElement( 'button', { type: 'submit', className: 'btn btn-dark btn-block', onClick: this.submitValidation }, 'Validate' )
				)
			);
		}
	};
} );