define( ["react"], function( React ) {
	return class Transaction extends React.Component {
		constructor( props ) {
			super( props );

			this.renderDescriptor = this.renderDescriptor.bind( this );
			this.renderBody = this.renderBody.bind( this );
		}

		renderDescriptor() {
			let transaction = this.props.transaction;
			let date = new Date( Number( transaction.body.timestamp ) );
			let installation = transaction.body.location.installation;
			let x = transaction.body.location.point.X;
			let y = transaction.body.location.point.Y;
			return React.createElement( 'summary', {}, date.toLocaleDateString() + ' ' + date.toLocaleTimeString() + ', Installation: ' + installation + ', [X:' + x + ', Y:' + y + ']' );
		}

		renderBody() {
			let transaction = this.props.transaction;
			return React.createElement( 'div', { className: 'transaction-body' },
				this.renderField( 'Transaction', React.createElement( 'a', { key: 'transaction', href: 'https://thetangle.org/transaction/' + transaction.transaction, target: '_blank' }, transaction.transaction ) ),
				this.renderField( 'Transaction key', transaction.key ),
				this.renderField( 'Sign key', React.createElement( 'a', { key: 'sign-key', href: 'https://pgp.mit.edu/pks/lookup?op=vindex&search=0x' + transaction.sign.key, target: '_blank' }, '0x' + transaction.sign.key ) ),
			);
		}


		renderField( label, value ) {
			return React.createElement( 'div', { className: 'field', key: label },
				React.createElement( 'span', { className: 'field-label' }, label ),
				React.createElement( 'span', { className: 'field-value' }, value )
			);
		}

		render() {
			return React.createElement( 'details', {},
				this.renderDescriptor(),
				this.renderBody()
			);
		}
	};
} );