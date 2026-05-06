<script lang="ts">
	interface Props {
		status: string;
	}

	let { status }: Props = $props();

	const styles: Record<string, { label: string; className: string }> = {
		PROCESSING: { label: 'Processing', className: 'bg-amber-100 text-amber-900' },
		COMPLETED: { label: 'Completed', className: 'bg-emerald-100 text-emerald-900' },
		COMPLETED_WITH_ERRORS: {
			label: 'Completed with errors',
			className: 'bg-rose-100 text-rose-900'
		},
		FAILED: { label: 'Failed', className: 'bg-zinc-200 text-zinc-800' }
	};

	let display = $derived(
		styles[status] ?? { label: status, className: 'bg-zinc-200 text-zinc-800' }
	);
</script>

<span
	class={`inline-flex items-center rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] ${display.className}`}
>
	{display.label}
</span>
