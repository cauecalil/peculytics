import { apiBaseUrl } from './config';
import type {
	AnalysisDetail,
	AnalysisListItem,
	CreateAnalysisResponse,
	SummaryResponse,
	TransactionsPage
} from './types';

export class ApiError extends Error {
	code: string;
	status: number;

	constructor(code: string, message: string, status: number) {
		super(message);
		this.code = code;
		this.status = status;
	}
}

const buildUrl = (path: string) => `${apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`;

const requestJson = async <T>(path: string, options: RequestInit = {}): Promise<T> => {
	const headers = new Headers(options.headers);
	if (!headers.has('Accept')) {
		headers.set('Accept', 'application/json');
	}

	const response = await fetch(buildUrl(path), { ...options, headers });
	const contentType = response.headers.get('content-type') ?? '';
	const isJson = contentType.includes('application/json') || contentType.includes('+json');
	const payload = isJson && response.status !== 204 ? await response.json() : null;

	if (!response.ok) {
		const message = payload?.detail ?? payload?.message ?? `Request failed with status ${response.status}.`;
		const code = payload?.type ?? payload?.error ?? payload?.title ?? response.statusText ?? 'REQUEST_FAILED';
		throw new ApiError(code, message, response.status);
	}

	return payload as T;
};

export const listAnalyses = () => requestJson<AnalysisListItem[]>('/analyses');

export const getAnalysis = (analysisId: string) =>
	requestJson<AnalysisDetail>(`/analyses/${analysisId}`);

export const getSummary = (analysisId: string) =>
	requestJson<SummaryResponse>(`/analyses/${analysisId}/summary`);

export const getTransactions = (analysisId: string, page: number, size: number) =>
	requestJson<TransactionsPage>(`/analyses/${analysisId}/transactions?page=${page}&size=${size}`);

export const deleteAnalysis = (analysisId: string) =>
	requestJson<void>(`/analyses/${analysisId}`, {
		method: 'DELETE'
	});

export interface CreateAnalysisInput {
	title: string;
	files: File[];
	fileTitles: string[];
}

export const createAnalysis = async (input: CreateAnalysisInput) => {
	const formData = new FormData();
	formData.append('title', input.title);
	input.files.forEach((file) => formData.append('files', file));
	input.fileTitles.forEach((title) => formData.append('fileTitles', title));

	return requestJson<CreateAnalysisResponse>('/analyses', {
		method: 'POST',
		body: formData
	});
};
