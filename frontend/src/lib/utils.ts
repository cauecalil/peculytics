import type { AnalysisListItem } from './types';

const dayFormatter = new Intl.DateTimeFormat('en-US', {
	weekday: 'long',
	month: 'long',
	day: 'numeric'
});

const dateFormatter = new Intl.DateTimeFormat('en-US', {
	month: 'short',
	day: 'numeric',
	year: 'numeric'
});

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
	month: 'short',
	day: 'numeric',
	year: 'numeric',
	hour: '2-digit',
	minute: '2-digit'
});

const numberFormatter = new Intl.NumberFormat('en-US');
const amountFormatter = new Intl.NumberFormat('en-US', {
	minimumFractionDigits: 2,
	maximumFractionDigits: 2
});
const percentFormatter = new Intl.NumberFormat('en-US', {
	minimumFractionDigits: 1,
	maximumFractionDigits: 1
});

const parseDate = (value: string) => {
	if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
		const [year, month, day] = value.split('-').map(Number);
		return new Date(year, month - 1, day);
	}

	return new Date(value);
};

const startOfDay = (value: Date) => {
	const date = new Date(value);
	date.setHours(0, 0, 0, 0);
	return date;
};

export interface AnalysisDayGroup {
	key: string;
	label: string;
	items: AnalysisListItem[];
}

export const labelForDay = (value: string) => {
	const date = new Date(value);
	const today = startOfDay(new Date());
	const target = startOfDay(date);
	const diffDays = Math.round((today.getTime() - target.getTime()) / 86400000);

	if (diffDays === 0) {
		return 'Today';
	}

	if (diffDays === 1) {
		return 'Yesterday';
	}

	return dayFormatter.format(date);
};

export const groupAnalysesByDay = (analyses: AnalysisListItem[]): AnalysisDayGroup[] => {
	const sorted = [...analyses].sort(
		(left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
	);
	const groups: AnalysisDayGroup[] = [];

	for (const analysis of sorted) {
		const dayKey = startOfDay(new Date(analysis.createdAt)).toISOString().slice(0, 10);
		const current = groups[groups.length - 1];

		if (!current || current.key !== dayKey) {
			groups.push({
				key: dayKey,
				label: labelForDay(analysis.createdAt),
				items: [analysis]
			});
		} else {
			current.items.push(analysis);
		}
	}

	return groups;
};

export const formatDate = (value: string) => dateFormatter.format(parseDate(value));
export const formatDateTime = (value: string) => dateTimeFormatter.format(new Date(value));
export const formatAmount = (value: number) => amountFormatter.format(value);
export const formatCount = (value: number) => numberFormatter.format(value);
export const formatPercentage = (value: number) => `${percentFormatter.format(value)}%`;
export const getErrorMessage = (error: unknown, fallback: string) =>
	error instanceof Error ? error.message : fallback;
