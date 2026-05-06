<script lang="ts">
	import { dismiss, notifications } from '$lib/stores/notifications';

	const styles: Record<string, string> = {
		error: 'border-[color:var(--rose)]/40 bg-[color:var(--rose)]/10 text-[color:var(--rose)]',
		info: 'border-[color:var(--teal)]/40 bg-[color:var(--teal)]/10 text-[color:var(--teal)]'
	};
</script>

<div class="pointer-events-none fixed right-6 top-6 z-50 flex max-w-sm flex-col gap-3">
	{#each $notifications as note (note.id)}
		<div
			class={`pointer-events-auto rounded-2xl border px-4 py-3 text-sm shadow-lg backdrop-blur ${
				styles[note.type] ?? styles.info
			}`}
		>
			<div class="flex items-start justify-between gap-3">
				<p class="leading-snug">{note.message}</p>
				<button
					class="text-xs uppercase tracking-[0.2em] opacity-70 transition hover:opacity-100"
					onclick={() => dismiss(note.id)}
					type="button"
				>
					Close
				</button>
			</div>
		</div>
	{/each}
</div>
