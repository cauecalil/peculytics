<script lang="ts">
	import { formatAmount, formatPercentage } from '$lib/utils';
	import type { SummaryCategory } from '$lib/types';

	interface Props {
		categories?: SummaryCategory[];
		total?: number;
	}

	let { categories = [], total = 0 }: Props = $props();

	const palette = ['#2f7b7b', '#f2b05e', '#e77c6c', '#6b8e7a', '#3f6e8c', '#b08b5e'];

	let segments = $derived(
		categories
			.filter((category) => category.total > 0)
			.map((category, index) => ({
				...category,
				percent: total > 0 ? (category.total / total) * 100 : 0,
				color: palette[index % palette.length]
			}))
	);

	let gradient = $derived.by(() => {
		if (segments.length === 0) {
			return 'conic-gradient(#e7dccf 0 100%)';
		}

		let offset = 0;
		const stops: string[] = [];
		for (const segment of segments) {
			const start = offset;
			const end = start + segment.percent;
			stops.push(`${segment.color} ${start}% ${end}%`);
			offset = end;
		}
		return `conic-gradient(${stops.join(', ')})`;
	});
</script>

<div class="flex flex-col gap-6 lg:flex-row lg:items-center">
	<div class="relative flex h-40 w-40 items-center justify-center">
		<div class="h-40 w-40 rounded-full" style={`background: ${gradient};`}></div>
		<div class="absolute inset-6 rounded-full bg-[color:var(--surface)] shadow-inner">
			<div class="flex h-full flex-col items-center justify-center text-center">
				<p class="text-[11px] uppercase tracking-[0.3em] text-muted">Total</p>
				<p class="text-lg font-semibold">{formatAmount(total)}</p>
			</div>
		</div>
	</div>
	<div class="flex-1 space-y-3">
		{#if segments.length === 0}
			<p class="text-sm text-muted">No expense data yet.</p>
		{:else}
			{#each segments as segment}
				<div class="flex items-center justify-between text-sm">
					<div class="flex items-center gap-2">
						<span
							class="h-2.5 w-2.5 rounded-full"
							style={`background: ${segment.color};`}
						></span>
						<span class="font-medium">{segment.category}</span>
					</div>
					<div class="flex items-center gap-3 text-xs text-muted">
						<span>{formatAmount(segment.total)}</span>
						<span>{formatPercentage(segment.percent)}</span>
					</div>
				</div>
			{/each}
		{/if}
	</div>
</div>
