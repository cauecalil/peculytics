import { env } from '$env/dynamic/public';

const fallbackBaseUrl = 'http://localhost:8080';

export const apiBaseUrl = (env.PUBLIC_API_BASE_URL || fallbackBaseUrl).replace(/\/$/, '');
