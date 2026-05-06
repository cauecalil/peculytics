export type AnalysisStatus = 'PROCESSING' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED';
export type StatementFileStatus = 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type TransactionCategory =
	| 'Food'
	| 'Transport'
	| 'Groceries'
	| 'Health'
	| 'Subscriptions'
	| 'Housing'
	| 'Utilities'
	| 'Shopping'
	| 'Education'
	| 'Income'
	| 'Other'
	| 'Uncategorized';
export type CategorySource = 'RULE' | 'AI' | 'FALLBACK' | null;

export interface AnalysisListItem {
	id: string;
	title: string;
	status: AnalysisStatus;
	totalFiles: number;
	totalTransactions: number;
	createdAt: string;
	completedAt: string | null;
}

export interface StatementFileSummary {
	id: string;
	title: string;
	fileName: string;
	status: StatementFileStatus;
	totalTransactions: number;
}

export interface AnalysisDetail {
	id: string;
	title: string;
	status: AnalysisStatus;
	totalFiles: number;
	totalTransactions: number;
	processedBatches: number;
	totalBatches: number;
	createdAt: string;
	completedAt: string | null;
	files: StatementFileSummary[];
}

export interface TransactionSourceFile {
	id: string;
	title: string;
}

export interface TransactionItem {
	id: string;
	transactionDate: string;
	description: string;
	amount: number;
	category: TransactionCategory | null;
	categorySource: CategorySource;
	sourceFile: TransactionSourceFile;
}

export interface TransactionsPage {
	content: TransactionItem[];
	totalElements: number;
	totalPages: number;
	page: number;
	size: number;
}

export interface SummaryCategory {
	category: TransactionCategory;
	total: number;
	percentage: number;
}

export interface SummaryResponse {
	analysisId: string;
	totalExpenses: number;
	categories: SummaryCategory[];
}

export interface CreateAnalysisResponse {
	id: string;
	title: string;
	status: AnalysisStatus;
	totalFiles: number;
	acceptedFiles: number;
	rejectedFiles: number;
	message: string;
}
