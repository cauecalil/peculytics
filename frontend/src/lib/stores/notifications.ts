import { writable } from 'svelte/store';

type NotificationType = 'error' | 'info';

export interface Notification {
	id: string;
	message: string;
	type: NotificationType;
}

const notificationsStore = writable<Notification[]>([]);
let idCounter = 0;
const lastShown = new Map<string, number>();

const createId = () => {
	idCounter += 1;
	return `${Date.now()}-${idCounter}`;
};

const dismissNotification = (id: string) => {
	notificationsStore.update((items) => items.filter((item) => item.id !== id));
};

const notify = (message: string, type: NotificationType, timeoutMs = 6000) => {
	const id = createId();
	notificationsStore.update((items) => [...items, { id, message, type }]);

	if (timeoutMs > 0) {
		setTimeout(() => dismissNotification(id), timeoutMs);
	}

	return id;
};

const shouldNotify = (message: string, cooldownMs: number) => {
	const now = Date.now();
	const last = lastShown.get(message);
	if (last && now - last < cooldownMs) {
		return false;
	}
	lastShown.set(message, now);
	return true;
};

export const notifyError = (message: string, cooldownMs = 8000) => {
	if (!shouldNotify(message, cooldownMs)) {
		return null;
	}

	return notify(message, 'error');
};

export const notifications = {
	subscribe: notificationsStore.subscribe
};

export const dismiss = dismissNotification;
