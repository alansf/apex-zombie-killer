import { LightningElement, api } from 'lwc';

export default class HerokuAppContainer extends LightningElement {
	@api appUrl; // Optional override
	src = 'https://apex-zombie-killer-6f48e437a14e.herokuapp.com/';

	connectedCallback() {
		if (this.appUrl && typeof this.appUrl === 'string') {
			this.src = this.appUrl;
		}
		this._onMessage = (event) => {
			// Optionally filter origin: event.origin === new URL(this.src).origin
			// Handle messages from embedded app if needed
			// e.g., this.dispatchEvent(new CustomEvent('herokuevent', { detail: event.data }));
		};
		window.addEventListener('message', this._onMessage);
	}

	disconnectedCallback() {
		if (this._onMessage) {
			window.removeEventListener('message', this._onMessage);
		}
	}
}


