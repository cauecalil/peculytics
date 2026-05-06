<script lang="ts">
	import { onMount } from 'svelte';
	import { listAnalyses } from '$lib/api';
	import { formatCount, formatDateTime, getErrorMessage, groupAnalysesByDay } from '$lib/utils';
	import type { AnalysisListItem } from '$lib/types';
	import StatusBadge from '$lib/components/StatusBadge.svelte';
	import { notifyError } from '$lib/stores/notifications';

	let analyses = $state<AnalysisListItem[]>([]);
	let loading = $state(true);

	const loadAnalyses = async () => {
		loading = true;

		try {
			analyses = await listAnalyses();
		} catch (error) {
			notifyError(getErrorMessage(error, 'Unable to load analyses.'));
		} finally {
			loading = false;
		}
	};

	onMount(loadAnalyses);

	let groups = $derived(groupAnalysesByDay(analyses));
</script>

<svelte:head>
	<title>Peculytics - Analyses</title>
</svelte:head>

<section class="space-y-10">
	<div class="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
		<div class="space-y-3">
			<p class="section-kicker">Overview</p>
			<h1 class="text-4xl sm:text-5xl">Analyses</h1>
			<p class="max-w-xl text-base text-muted">
				Upload CSV statements, let the system categorize transactions, and track results in one
				place.
			</p>
		</div>
	</div>

	{#if loading}
		<div class="card animate-rise p-6">
			<p class="text-sm text-muted">Loading analyses...</p>
		</div>
	{:else if groups.length === 0}
		<div class="card animate-rise p-8 text-center">
			<p class="text-lg font-semibold">No analyses yet</p>
			<p class="mt-2 text-sm text-muted">
				Create your first analysis to start exploring transactions.
			</p>
			<a class="btn-primary mt-6 inline-flex" href="/analyses/new">Create analysis</a>
		</div>
	{:else}
		<div class="space-y-10">
			{#each groups as group}
				<div class="space-y-4">
					<div class="flex items-center gap-4">
						<span class="section-kicker">{group.label}</span>
						<div class="h-px flex-1 bg-[color:var(--line)]"></div>
					</div>
					<div class="grid gap-4">
						{#each group.items as analysis, index (analysis.id)}
							<a
								href={`/analyses/${analysis.id}`}
								class="card group animate-rise p-6 transition hover:-translate-y-1 hover:border-[color:var(--accent)]"
								style={`animation-delay: ${index * 60}ms`}
							>
								<div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
									<div class="space-y-1">
										<h2 class="text-2xl font-semibold">{analysis.title}</h2>
										<p class="text-sm text-muted">
											Created {formatDateTime(analysis.createdAt)}
										</p>
									</div>
									<StatusBadge status={analysis.status} />
								</div>
								<div class="mt-4 flex flex-wrap items-center gap-4 text-sm text-muted">
									<span>{formatCount(analysis.totalFiles)} files</span>
									<span>
										{analysis.totalTransactions > 0
											? `${formatCount(analysis.totalTransactions)} transactions`
											: analysis.status === 'PROCESSING'
												? 'Transactions pending'
												: '0 transactions'}
									</span>
								</div>
							</a>
						{/each}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</section>
