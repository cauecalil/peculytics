<script lang="ts">
	import { onDestroy, onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { deleteAnalysis, getAnalysis, getSummary, getTransactions } from '$lib/api';
	import {
		formatAmount,
		formatCount,
		formatDate,
		formatDateTime,
		getErrorMessage
	} from '$lib/utils';
	import StatusBadge from '$lib/components/StatusBadge.svelte';
	import PieChart from '$lib/components/PieChart.svelte';
	import type { AnalysisDetail, SummaryResponse, TransactionsPage } from '$lib/types';
	import { notifyError } from '$lib/stores/notifications';

	let analysis = $state<AnalysisDetail | null>(null);
	let summary = $state<SummaryResponse | null>(null);
	let transactions = $state<TransactionsPage | null>(null);
	let loading = $state(true);
	let pageIndex = 0;
	const pageSize = 50;
	let refreshTimer: ReturnType<typeof setInterval> | null = null;
	let refreshing = $state(false);
	let deleting = $state(false);

	let analysisId = $derived($page.params.id);

	const isProcessing = () => analysis?.status === 'PROCESSING';
	const canDeleteAnalysis = () => analysis !== null && !isProcessing();

	const stopPolling = () => {
		if (refreshTimer) {
			clearInterval(refreshTimer);
			refreshTimer = null;
		}
	};

	const refreshAll = async (showLoader = false) => {
		if (!analysisId || refreshing || deleting) {
			return;
		}

		refreshing = true;
		if (showLoader) {
			loading = true;
		}

		try {
			analysis = await getAnalysis(analysisId);
		} catch (error) {
			stopPolling();
			loading = false;
			refreshing = false;
			if (!deleting) {
				notifyError(getErrorMessage(error, 'Unable to load analysis.'));
			}
			return;
		}

		if (deleting) {
			loading = false;
			refreshing = false;
			return;
		}

		try {
			summary = await getSummary(analysisId);
		} catch (error) {
			if (!deleting && !isProcessing()) {
				notifyError(getErrorMessage(error, 'Unable to load summary.'));
			}
		}

		try {
			transactions = await getTransactions(analysisId, pageIndex, pageSize);
			pageIndex = transactions.page;
		} catch (error) {
			if (!deleting && !isProcessing()) {
				notifyError(getErrorMessage(error, 'Unable to load transactions.'));
			}
		}

		if (!isProcessing()) {
			stopPolling();
		}

		loading = false;
		refreshing = false;
	};

	const loadTransactions = async (nextPage: number) => {
		if (!analysisId || deleting) {
			return;
		}

		try {
			transactions = await getTransactions(analysisId, nextPage, pageSize);
			pageIndex = transactions.page;
		} catch (error) {
			if (!deleting) {
				notifyError(getErrorMessage(error, 'Unable to load transactions.'));
			}
		}
	};

	const changePage = async (nextPage: number) => {
		if (!transactions) {
			return;
		}

		if (nextPage < 0 || nextPage >= transactions.totalPages) {
			return;
		}

		await loadTransactions(nextPage);
	};

	const changeToPreviousPage = async () => {
		if (!transactions) {
			return;
		}

		await changePage(transactions.page - 1);
	};

	const changeToNextPage = async () => {
		if (!transactions) {
			return;
		}

		await changePage(transactions.page + 1);
	};

	const startPolling = () => {
		if (refreshTimer || !isProcessing()) {
			return;
		}

		refreshTimer = setInterval(async () => {
			if (deleting || !isProcessing()) {
				stopPolling();
				return;
			}

			if (!analysisId || refreshing) {
				return;
			}
			await refreshAll(false);
		}, 5000);
	};

	const deleteCurrentAnalysis = async () => {
		if (!analysisId || deleting) {
			return;
		}

		if (!canDeleteAnalysis()) {
			notifyError('Analysis cannot be deleted while processing.');
			return;
		}

		const confirmed = confirm(
			`Delete "${analysis?.title ?? 'this analysis'}"? This will permanently remove its files and transactions.`
		);
		if (!confirmed) {
			return;
		}

		deleting = true;
		stopPolling();

		try {
			await deleteAnalysis(analysisId);
			stopPolling();
			await goto('/');
		} catch (error) {
			stopPolling();
			notifyError(getErrorMessage(error, 'Unable to delete analysis.'));
			deleting = false;
		}
	};

	onMount(async () => {
		await refreshAll(true);
		if (isProcessing()) {
			startPolling();
		}
	});

	onDestroy(() => {
		stopPolling();
	});
</script>

<svelte:head>
	<title>Peculytics - {analysis?.title ?? 'Analysis'}</title>
</svelte:head>

{#if loading && !analysis}
	<div class="card p-6 animate-rise">
		<p class="text-sm text-muted">Loading analysis...</p>
	</div>
{:else}
	<section class="space-y-10 animate-rise">
		<div class="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
			<div class="space-y-3">
				<p class="section-kicker">Analysis</p>
				<h1 class="text-4xl sm:text-5xl">{analysis?.title ?? 'Analysis'}</h1>
				<div class="flex flex-wrap items-center gap-4 text-sm text-muted">
					<span>
						Created {analysis ? formatDateTime(analysis.createdAt) : '--'}
					</span>
					{#if analysis?.completedAt}
						<span>Completed {formatDateTime(analysis.completedAt)}</span>
					{/if}
				</div>
			</div>
			{#if analysis}
				<div class="flex flex-wrap items-center gap-3 sm:justify-end">
					<StatusBadge status={analysis.status} />
					<button
						type="button"
						class="btn-outline border-[color:var(--rose)]/50 text-[color:var(--rose)] hover:border-[color:var(--rose)] hover:text-[color:var(--rose)] disabled:cursor-not-allowed disabled:opacity-50"
						disabled={deleting || !canDeleteAnalysis()}
						title={isProcessing()
							? 'Analysis cannot be deleted while processing.'
							: 'Delete analysis'}
						onclick={deleteCurrentAnalysis}
					>
						{deleting ? 'Deleting...' : 'Delete analysis'}
					</button>
				</div>
			{/if}
		</div>

		{#if analysis?.status === 'PROCESSING'}
			<div class="card flex items-start gap-3 p-4">
				<span class="mt-1 h-2 w-2 rounded-full bg-[color:var(--accent-strong)] animate-slow-pulse"></span>
				<p class="text-sm text-muted">
					Processing files and categorizing transactions. This page updates automatically.
				</p>
			</div>
		{/if}

		{#if analysis?.status === 'COMPLETED_WITH_ERRORS'}
			<div class="card border-[color:var(--rose)]/40 bg-[color:var(--rose)]/10 p-4">
				<p class="text-sm font-semibold text-[color:var(--rose)]">Completed with errors</p>
				<p class="text-sm text-muted">
					Some files or batches failed, but transactions are still available.
				</p>
			</div>
		{/if}

		<div class="grid gap-4 lg:grid-cols-3">
			<div class="card p-6">
				<p class="text-xs uppercase tracking-[0.3em] text-muted">Files</p>
				<p class="mt-3 text-3xl font-semibold">
					{analysis ? formatCount(analysis.totalFiles) : '--'}
				</p>
			</div>
			<div class="card p-6">
				<p class="text-xs uppercase tracking-[0.3em] text-muted">Transactions</p>
				<p class="mt-3 text-3xl font-semibold">
					{analysis ? formatCount(analysis.totalTransactions) : '--'}
				</p>
			</div>
			<div class="card p-6">
				<p class="text-xs uppercase tracking-[0.3em] text-muted">Batches</p>
				<p class="mt-3 text-3xl font-semibold">
					{analysis
						? analysis.totalBatches > 0
							? `${analysis.processedBatches}/${analysis.totalBatches}`
							: '0'
						: '--'}
				</p>
			</div>
		</div>

		<div class="card space-y-6 p-6">
			<div class="flex flex-col gap-2">
				<p class="section-kicker">Statement files</p>
				<h2 class="text-2xl">Uploaded files</h2>
				<p class="text-sm text-muted">Each file shows its own status and transaction count.</p>
			</div>
			<div class="grid gap-3">
				{#if analysis && analysis.files.length > 0}
					{#each analysis.files as file (file.id)}
						<div class="flex flex-col gap-3 rounded-2xl border border-[color:var(--line)]/60 bg-white/60 p-4 sm:flex-row sm:items-center sm:justify-between">
							<div>
								<p class="text-base font-semibold">{file.title}</p>
								<p class="text-xs text-muted">{file.fileName}</p>
							</div>
							<div class="flex items-center gap-4">
								<StatusBadge status={file.status} />
								<span class="text-sm text-muted">{formatCount(file.totalTransactions)} tx</span>
							</div>
						</div>
					{/each}
				{:else}
					<div class="rounded-2xl border border-dashed border-[color:var(--line)]/70 bg-white/50 p-6">
						<p class="text-sm text-muted">No files available yet.</p>
					</div>
				{/if}
			</div>
		</div>

		<div class="card space-y-6 p-6">
			<div class="flex flex-col gap-2">
				<p class="section-kicker">Summary</p>
				<h2 class="text-2xl">Expense breakdown</h2>
				<p class="text-sm text-muted">Only negative amounts are included in this chart.</p>
			</div>
			<PieChart categories={summary?.categories ?? []} total={summary?.totalExpenses ?? 0} />
		</div>

		<div class="card space-y-6 p-6">
			<div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
				<div>
					<p class="section-kicker">Transactions</p>
					<h2 class="text-2xl">All transactions</h2>
					<p class="text-sm text-muted">Sorted by date and id, newest first.</p>
				</div>
				{#if refreshing}
					<span class="text-xs text-muted">Refreshing...</span>
				{/if}
			</div>

			{#if transactions && transactions.content.length > 0}
				<div class="overflow-x-auto">
					<table class="min-w-full text-left text-sm">
						<thead>
							<tr class="text-xs uppercase tracking-[0.2em] text-muted">
								<th class="pb-3 pr-6">Date</th>
								<th class="pb-3 pr-6">Description</th>
								<th class="pb-3 pr-6">Amount</th>
								<th class="pb-3 pr-6">Category</th>
								<th class="pb-3">Source</th>
							</tr>
						</thead>
						<tbody>
							{#each transactions.content as transaction (transaction.id)}
								<tr class="border-t border-[color:var(--line)]/60">
									<td class="py-3 pr-6 whitespace-nowrap">
										{formatDate(transaction.transactionDate)}
									</td>
									<td class="py-3 pr-6 min-w-[200px]">{transaction.description}</td>
									<td class="py-3 pr-6 whitespace-nowrap">
										<span
											class={`font-semibold ${
												transaction.amount < 0
													? 'text-[color:var(--rose)]'
													: 'text-[color:var(--teal)]'
											}`}
										>
											{transaction.amount < 0 ? '-' : '+'}
											{formatAmount(Math.abs(transaction.amount))}
										</span>
									</td>
									<td class="py-3 pr-6">
										<div class="flex flex-col">
											<span class="font-medium">{transaction.category ?? 'Pending'}</span>
											{#if transaction.categorySource}
												<span class="text-xs text-muted">
													{transaction.categorySource}
												</span>
											{/if}
										</div>
									</td>
									<td class="py-3 text-sm text-muted">
										{transaction.sourceFile?.title ?? '-'}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>

				{#if transactions.totalPages > 1}
					<div class="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
						<p class="text-muted">
							Page {transactions.page + 1} of {transactions.totalPages}
						</p>
						<div class="flex items-center gap-2">
							<button
								class="btn-outline disabled:cursor-not-allowed disabled:opacity-40"
								disabled={transactions.page === 0}
								onclick={changeToPreviousPage}
							>
								Previous
							</button>
							<button
								class="btn-outline disabled:cursor-not-allowed disabled:opacity-40"
								disabled={transactions.page + 1 >= transactions.totalPages}
								onclick={changeToNextPage}
							>
								Next
							</button>
						</div>
					</div>
				{/if}
			{:else}
				<div class="rounded-2xl border border-dashed border-[color:var(--line)]/70 bg-white/50 p-6">
					<p class="text-sm text-muted">No transactions available yet.</p>
				</div>
			{/if}
		</div>
	</section>
{/if}
